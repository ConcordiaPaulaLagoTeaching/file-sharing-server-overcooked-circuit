package ca.concordia.filesystem;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

import ca.concordia.filesystem.datastructures.FEntry;

public class FileSystemManager {

    private static final int BLOCK_SIZE = 128; // Example block size
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;

    private static FileSystemManager instance = null; // no instance at first
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws Exception { // Could not do a
                                                                                // try catch block
        if (instance == null) {
            instance = this;
            disk = new RandomAccessFile(filename, "rw");
            disk.setLength(totalSize);

            inodeTable = new FEntry[MAXFILES];
            freeBlockList = new boolean[MAXBLOCKS];
            freeBlockList[0] = true; // first block for MAtadata

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    public void createFile(String fileName) throws Exception {
        globalLock.lock();

        try {
            for (FEntry entry : inodeTable) {
                if (entry != null && fileName.equalsIgnoreCase(entry.getFilename())) {
                    throw new Exception("File \"" + fileName + "\" already exists.");
                }
            }

            for (int i = 0; i < inodeTable.length; i++) {
                if (inodeTable[i] != null) {
                    // Casting the numbers because the compiler is screaming at me :(
                    FEntry file = new FEntry(fileName, (short) 0, (short) -1);
                    inodeTable[i] = file;

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
            for (FEntry entry : inodeTable) {
                if (entry != null && entry.getFilename().equalsIgnoreCase(fileName)) {
                    file = entry;
                }
            }

            if (file == null) {
                throw new Exception("File not found...");
            }

            // Check if there is space available.
            int availableSpace = 0;
            int numBlocks = (int) Math.ceil((double)contents.length / BLOCK_SIZE);
            for (boolean block : freeBlockList) {
                if (!block) {
                    availableSpace++;
                }
            }
            
            if (availableSpace < numBlocks) {
                throw new Exception("Not enough free space...");
            }

            /*
            Note to self: 
            1. free the old block and write new block
            2. If another node is there go there and continue writing. If writing is done delete all subsequent nodes and set all to 0.
            3. Update the metadata part
            */
        } finally {
            globalLock.unlock();
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
