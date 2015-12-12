////////need to test line 45
public class Inode{
    private final static int iNodeSize = 32;
    private final static int directSize = 11;

    public int length;
    public short count;
    public short flag;
    public short direct[] = new short[directSize];
    public short indirect;

    public Inode(){
        length = 0;
        count = 0;
        flag = 1;
        for(int i = 0; i< directSize ; i++){
            direct[i] = -1;
        }
        indirect = -1;
    }

    //16 inodes per block
    public Inode(short iNumber){
        int bNumber = ((int)Math.ceil(iNumber/16.0));//roundup
        byte[]info = new byte[Disk.blockSize];
        SysLib.rawread(bNumber,info);

        //locate where in the block, int=4 bytes, short = 2bytes,
        int offset = ((iNumber % 16) * iNodeSize);

        //set all variables from the offset
        length = SysLib.bytes2short(info, offset);
        offset += 4;
        count = SysLib.bytes2short(info, offset);
        offset += 2;
        flag = SysLib.bytes2short(info, offset);
        offset += 2;
        //set the direct[]
        for(int i = 0; i < directSize; i++){
            direct[i]= SysLib.bytes2short(info, offset);
            offset += 2;
        }
        //set indirect
        indirect = SysLib.bytes2short(info, offset);
        offset += 2; /////////////need to test if needed or not?????
    }

    //save the ith number inode into disk
    public void toDisk(short iNumber){
        int bNumber = ((int)Math.ceil(iNumber/16.0));//roundup
        int offset = 0;
        byte[]info = new byte[iNodeSize];

        SysLib.int2bytes(length, info, offset);
        offset += 4;
        SysLib.short2bytes(count, info, offset);
        offset += 2;
        SysLib.short2bytes(flag, info, offset);
        offset+= 2;
        for(int i = 0; i < directSize; i++){
            SysLib.short2bytes(direct[i], info, offset);
            offset += 2;
        }

        SysLib.short2bytes(indirect, info, offset);
        byte[] newInfo = new byte[Disk.blockSize];
        //get the block old info
        SysLib.rawread(bNumber,newInfo);
        System.arraycopy(info, 0, newInfo, (((iNumber % 16) * iNodeSize)), iNodeSize);
        SysLib.rawwrite(bNumber, newInfo);

    }
}