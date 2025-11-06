package ca.concordia.filesystem;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

public class FileSystemManager {

    private static final int BLOCK_SIZE = 128; // Example block size
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;

    private static FileSystemManager instance = null; // no instance at first
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private FEntry[] fEntryTable; // Array of inodes
    private FNode[] fNodeTable;
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws Exception { // Could not do a
                                                                                // try catch block
        if (instance == null) {
            instance = this;
            disk = new RandomAccessFile(filename, "rw");
            disk.setLength(totalSize);

            fEntryTable = new FEntry[MAXFILES];
            freeBlockList = new boolean[MAXBLOCKS];
            freeBlockList[0] = true; // first block for MAtadata

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    public void createFile(String fileName) throws Exception {
        globalLock.lock();

        try {
            for (FEntry entry : fEntryTable) {
                if (entry != null && fileName.equalsIgnoreCase(entry.getFilename())) {
                    throw new Exception("File \"" + fileName + "\" already exists.");
                }
            }

            for (int i = 0; i < fEntryTable.length; i++) {
                if (fEntryTable[i] != null) {
                    // Casting the numbers because the compiler is screaming at me :(
                    FEntry file = new FEntry(fileName, (short) 0, (short) -1);
                    fEntryTable[i] = file;

                    System.out.println("File \"" + fileName + "\" was created.");
                    return;
                }
            }

            throw new Exception("Max file limit reached. File creation aborted...");

        } finally {
            globalLock.unlock();
        }
    }

    public void writeFile(String fileName, byte[] contents) throws Exception {
        globalLock.lock();

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
            ArrayList<Integer> usableBlockList = new ArrayList<>();
            for (int i = block; i != -1;) {
                int temp = i;
                usableBlockList.add(i);
                i = fNodeTable[i].getNext();
                fNodeTable[temp].setNext(-1);
            }

            // Collect all the free blocks and put it in the usable list
            for (int i = 0; i < freeBlockList.length; i++) {
                if (!freeBlockList[i]) {
                    usableBlockList.add(i);
                }
            }

            // Check if space is enougth
            int numBlocks = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            if (usableBlockList.size() < numBlocks) {
                throw new Exception("Not enough free space. Aborting...");
            }

            // Clear the blocks we are about to use
            for (int i = 0; i < numBlocks; i++) {
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
            globalLock.unlock();
        }
    }

    private void writeBlock(int block, byte[] content, int offset, int size) throws Exception {
        disk.seek(block * BLOCK_SIZE);

        // Set to zero if empty
        if (size == 0) {
            disk.write(new byte[BLOCK_SIZE]);
            freeBlockList[block] = true;

        } else {
            disk.write(content, offset, size);
            freeBlockList[block] = false;
        }
    }

    public byte[] readFile(String fileName) throws Exception {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public void deleteFile(String fileName) throws Exception {
        // globalLock.lock();

        // try {
        // int index = -1;
        // FEntry toDelete = null;

        // for (int i = 0; i < inodeTable.length; i++) {
        // FEntry entry = inodeTable[i];

        // if (entry != null && entry.getFilename().equalsIgnoreCase(fileName)) {
        // index = i;
        // toDelete = entry;
        // break;
        // }
        // }

        // if (toDelete == null) {
        // throw new Exception("File not found...");
        // }

        // } finally {
        // globalLock.unlock();
        // }
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public String[] listFiles() throws Exception {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
}
