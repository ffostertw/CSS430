public class FileTableEntry{
    public int seekPtr;
    public final Inode inode;
    public final short iNum;
    public int count;
    public final String mode;

    public FileTableEntry(Inode i, short iNumber, String mo){
        seekPtr = 0;
        inode = i;
        iNum = iNumber;
        count = 1;
        mode = mo;
        if(mode.compareToIgnoreCase("a") == 0){
            seekPtr = inode.length;
        }
    }

}
