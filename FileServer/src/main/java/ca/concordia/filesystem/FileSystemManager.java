package ca.concordia.filesystem;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

public class FileSystemManager {

    private static final int BLOCK_SIZE = 128; // Example block size
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;

    private static FileSystemManager instance = null; // no instance at first
    private final RandomAccessFile disk;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Object instanceLock = new Object();

    private FEntry[] fEntryTable; // Array of inodes
    private FNode[] fNodeTable;
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws Exception { // Could not do a
        synchronized (instanceLock) {
            if (instance != null) {
                throw new IllegalStateException("FileSystemManager already initialized");
            }
            instance = this;
            disk = new RandomAccessFile(filename, "rw");
            disk.setLength(totalSize);

            //Populating the arrays
            fEntryTable = new FEntry[MAXFILES];
            fNodeTable = new FNode[MAXBLOCKS];
            freeBlockList = new boolean[MAXBLOCKS];

            for (int i = 0; i < MAXBLOCKS; i++) {
                fNodeTable[i] = new FNode(i);
                freeBlockList[i] = true;
            }
            freeBlockList[0] = false; // first block for Metadata

            if (disk.length() > 0) {
                readMetaData();
            } else {
                writeMetaData();
            }
        }
    }

    public void createFile(String fileName) throws Exception {
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + "Waiting for write lock on " + fileName);
        rwLock.writeLock().lock();
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + "Acquired for write lock on " + fileName);

        try {
            for (FEntry entry : fEntryTable) {
                if (entry != null && fileName.equalsIgnoreCase(entry.getFilename())) {
                    throw new Exception("File \"" + fileName + "\" already exists.");
                }
            }

            for (int i = 0; i < fEntryTable.length; i++) {
                if (fEntryTable[i] == null) {
                    // Casting the numbers because the compiler is screaming at me :(
                    FEntry file = new FEntry(fileName, 0, -1);
                    fEntryTable[i] = file;

                    System.out.println("File \"" + fileName + "\" was created.");
                    return;
                }
            }

            throw new Exception("Max file limit reached. File creation aborted...");

        } finally {
            writeMetaData();

            System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                    + "Releasing write lock on " + fileName);
            rwLock.writeLock().unlock();
        }
    }

    public void writeFile(String fileName, byte[] contents) throws Exception {
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + " Waiting for Write lock on " + fileName);
        rwLock.writeLock().lock();
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + "Acquired Write lock on " + fileName);
                
        try {
            FEntry file = null;

            // Get the entry if its there
            for (FEntry entry : fEntryTable) {
                if (entry != null && entry.getFilename().equalsIgnoreCase(fileName)) {
                    file = entry;
                }
            }

            if (file == null) {
                throw new Exception("File not found...");
            }

            // Collect all the old used blocks and sets all next to -1.
            int block = file.getFirstBlock();
            int numOldBlocks = 0;
            ArrayList<Integer> usableBlockList = new ArrayList<>();
            for (int i = block; i != -1;) {
                int temp = i;
                usableBlockList.add(i);
                i = fNodeTable[i].getNext();
                fNodeTable[temp].setNext(-1);
                numOldBlocks++;
            }

            // Collect all the free blocks and put it in the usable list
            for (int i = 0; i < freeBlockList.length; i++) {
                if (freeBlockList[i]) {
                    usableBlockList.add(i);
                }
            }

            // Check if space is enougth
            int numBlocks = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            if (usableBlockList.size() < numBlocks) {
                throw new Exception("Not enough free space. Aborting...");
            }

            // clear old data blocks / the one we are going to use
            int clearAmmount = Math.max(numOldBlocks, numBlocks);
            for (int i = 0; i < clearAmmount; i++) {
                writeBlock(usableBlockList.get(i), new byte[BLOCK_SIZE], 0, BLOCK_SIZE);
            }

            // Start writing blocks
            int writeBlockRemaining = numBlocks;
            try {
                int i = 0;
                int offset = 0;
                while (writeBlockRemaining > 0) {
                    int byteToWrite = Math.min(BLOCK_SIZE, contents.length - offset);
                    writeBlock(usableBlockList.get(i), contents, offset, byteToWrite);
                    offset += byteToWrite;
                    writeBlockRemaining--;
                    i++;
                }

            } catch (Exception e) {
                throw e;
            }

            // Chain all the nodes together
            for (int i = 0; i < numBlocks; i++) {
                int currentNode = usableBlockList.get(i);

                int nextNode = (i == numBlocks - 1) ? -1 : usableBlockList.get(i + 1);
                fNodeTable[currentNode].setNext(nextNode);
            }

            // Setting the metadata
            for (int i = 0; i < numBlocks; i++) {
                freeBlockList[usableBlockList.get(i)] = false;
            }

            for (int i = numBlocks; i < usableBlockList.size(); i++) {
                freeBlockList[usableBlockList.get(i)] = true;
                fNodeTable[usableBlockList.get(i)].setNext(-1);
            }

            file.setFirstBlock(usableBlockList.get(0));
            file.setFilesize(contents.length);
        } finally {
            writeMetaData();
            
            System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                    + " Released write lock on " + fileName);
            rwLock.writeLock().unlock();
        }
    }

    public byte[] readFile(String fileName) throws Exception {
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + " Waiting for Read lock on " + fileName);
        rwLock.readLock().lock();
        System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                + "Acquired Read lock on " + fileName);

        try {
            FEntry file = null;

            // Get the entry if its there
            for (FEntry entry : fEntryTable) {
                if (entry != null && entry.getFilename().equalsIgnoreCase(fileName)) {
                    file = entry;
                }
            }

            if (file == null) {
                throw new Exception("File not found...");
            }

            // if a file is created but not written to for some reason
            if (file.getFirstBlock() == -1 || file.getFilesize() == 0) {
                return new byte[0];
            }

            // Set up to receive
            int fileSize = file.getFilesize();
            byte[] data = new byte[fileSize];

            // Start read
            int block = file.getFirstBlock();
            int offset = 0;

            while (block != -1 && offset < fileSize) {
                byte[] buffer = new byte[BLOCK_SIZE];
                disk.seek(block * BLOCK_SIZE);
                disk.read(buffer, 0, BLOCK_SIZE);

                int parseAmmount = Math.min(BLOCK_SIZE, fileSize - offset);
                System.arraycopy(buffer, 0, data, offset, parseAmmount); // Only parse actual data
                                                                         // into data

                block = fNodeTable[block].getNext();
                offset += parseAmmount;
            }

            return data;

        } finally {
            System.out.println("[Lock] Thread " + Thread.currentThread().getName()
                    + " Released Read lock on " + fileName);
            rwLock.readLock().unlock();
        }
    }

    public void deleteFile(String fileName) throws Exception {
        rwLock.writeLock().lock();

        try {
            int index = -1;
            FEntry toDelete = null;

            for (int i = 0; i < fEntryTable.length; i++) {
                FEntry entry = fEntryTable[i];

                if (entry != null && entry.getFilename().equalsIgnoreCase(fileName)) {
                    index = i;
                    toDelete = entry;
                    break;
                }
            }

            if (toDelete == null) {
                throw new Exception("File not found...");
            }

            int block = toDelete.getFirstBlock();
            while (block != -1) {
                freeBlockList[block] = true;

                int nextBlock = fNodeTable[block].getNext();

                fNodeTable[block].setNext(-1);
                writeBlock(block, new byte[BLOCK_SIZE], 0, BLOCK_SIZE);

                block = nextBlock;
            }

            toDelete.setFilesize(0);
            toDelete.setFirstBlock(-1);
            fEntryTable[index] = null;

        } finally {
            writeMetaData();
            
            rwLock.writeLock().unlock();
        }
    }

    public String[] listFiles() throws Exception {
        rwLock.readLock().lock();

        try {
            ArrayList<String> fileList = new ArrayList<>();
            for (FEntry entry : fEntryTable) {
                if (entry != null) {
                    fileList.add(entry.getFilename());
                }
            }
            return fileList.toArray(new String[0]);

        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void writeBlock(int block, byte[] content, int offset, int size) throws Exception {
        disk.seek(block * BLOCK_SIZE);
        disk.write(content, offset, size);
    }

    private void readMetaData() throws Exception {
        disk.seek(0);

        // Fentry
        for (int i = 0; i < MAXFILES; i++) {
            byte[] nameByte = new byte[11];
            disk.read(nameByte);
            String name = new String(nameByte).trim();

            int size = disk.readInt();
            int firstBlock = disk.readInt();

            if (!name.isEmpty()) {
                fEntryTable[i] = new FEntry(name, size, firstBlock);
            }
        }

        // FNode
        for (int i = 0; i < MAXBLOCKS; i++) {
            int next = disk.readInt();
            fNodeTable[i].setNext(next);
        }

        // FreeBlock list
        for (int i = 0; i < MAXBLOCKS; i++) {
            freeBlockList[i] = disk.readBoolean();
        }
    }

    private void writeMetaData() throws Exception {
        disk.seek(0);

        // FEntry
        for (int i = 0; i < MAXFILES; i++) {
            FEntry entry = fEntryTable[i];

            if (entry != null) {
                byte[] nameBytes = new byte[11];
                byte[] actual = entry.getFilename().getBytes();
                System.arraycopy(actual, 0, nameBytes, 0, Math.min(actual.length, 11));

                disk.write(nameBytes);
                disk.writeInt(entry.getFilesize());
                disk.writeInt(entry.getFirstBlock());
            }
        }

        // FNode
        for (int i = 0; i < MAXBLOCKS; i++) {
            disk.writeInt(fNodeTable[i].getNext());
        }

        // Free block list
        for (int i = 0; i < MAXBLOCKS; i++) {
            disk.writeBoolean(freeBlockList[i]);
        }
    }
}
