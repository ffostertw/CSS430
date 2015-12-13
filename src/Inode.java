////////need to test line 45
public class Inode
{
    private final int INODES_PER_BLOCK = 16;
    private final int INODE_SIZE = 32;
    private final int DIRECT_SIZE = 11;

    public int length;
    public short count;
    public short flag;
    public short direct[] = new short[DIRECT_SIZE];
    public short indirect;

    public Inode()
    {
        // Reset all info
        length = 0;
        count = 0;
        flag = 1;

        for(int i = 0; i < DIRECT_SIZE ; i++){
            direct[i] = -1;
        }
        indirect = -1;
    }

    public Inode(short iNumber){

        // Get the block number and
        int bNumber = iNumber/INODES_PER_BLOCK;

        // Retrive inode data
        byte[] info = new byte[Disk.blockSize];
        SysLib.rawread(bNumber,info);

        //locate ith inode in the block, int=4 bytes, short = 2bytes,
        int offset = ((iNumber % INODES_PER_BLOCK) * INODE_SIZE);

        //set all variables from the offset
        length = SysLib.bytes2int(info, offset);
        offset += 4;

        count = SysLib.bytes2short(info, offset);
        offset += 2;

        flag = SysLib.bytes2short(info, offset);
        offset += 2;

        //set the direct[]
        for(int i = 0; i < DIRECT_SIZE; i++)
        {
            direct[i]= SysLib.bytes2short(info, offset);
            offset += 2;
        }

        //set indirect
        indirect = SysLib.bytes2short(info, offset);
        offset += 2; /////////////need to test if needed or not?????
    }

    // Save ith inode to disk
    public void toDisk(short iNumber)
    {
        int bNumber = iNumber/INODES_PER_BLOCK;
        int offset = 0;
        byte[] info = new byte[INODE_SIZE];

        // Write data to template btye block
        SysLib.int2bytes(length, info, offset);
        offset += 4;

        SysLib.short2bytes(count, info, offset);
        offset += 2;

        SysLib.short2bytes(flag, info, offset);
        offset+= 2;

        for(int i = 0; i < DIRECT_SIZE; i++)
        {
            SysLib.short2bytes(direct[i], info, offset);
            offset += 2;
        }
        SysLib.short2bytes(indirect, info, offset);

        // Merge new data in this inode with the data in block
        byte[] newInfo = new byte[Disk.blockSize];
        SysLib.rawread(bNumber,newInfo);
        System.arraycopy(info, 0, newInfo, (((iNumber % INODES_PER_BLOCK) * INODE_SIZE)), INODE_SIZE);

        // Write back to block
        SysLib.rawwrite(bNumber, newInfo);

    }
}