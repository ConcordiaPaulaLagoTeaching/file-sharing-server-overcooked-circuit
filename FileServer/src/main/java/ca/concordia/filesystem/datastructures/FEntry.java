package ca.concordia.filesystem.datastructures;

public class FEntry {

    private String filename;
    private int filesize;
    private int firstBlock; // Pointers to data blocks

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException{
        //Check filename is max 11 bytes long
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public int getFilesize() {
        return filesize;
    }

    public void setFilesize(int filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public void setFirstBlock(int block) {
        firstBlock = block;
    }

    public int getFirstBlock() {
        return firstBlock;
    }
}
