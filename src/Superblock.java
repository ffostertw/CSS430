
public class SuperBlock
{
    private final int DEFAULT_TOTAL_INODES = 64;
    private final int INODE_SIZE = 32;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public SuperBlock(int diskSize)
    {
        // Retrive SuperBlcok data
        byte[] info = new byte[Disk.blockSize];
        SysLib.rawread(0,info);

        //set all variables from the offset
        totalBlocks = diskSize;

        int offset = 4;

        totalInodes = SysLib.bytes2int(info, offset);
        offset += 4;

        freeList = SysLib.bytes2int(info, offset);
        offset += 4;

        if (totalInodes <= 0 || freeList < 2)
        {
			format(64);
        }
    }
    public void format(int size)
    {
        byte[] info = new byte[Disk.blockSize];

        // Format inodes
        totalInodes = size;
        for (int i = 0; i < totalInodes; ++i)
        {
            // Reset inode in disk
            Inode inode = new Inode();
            inode.flag = 0;
            inode.toDisk((short)i);
        }

        // Format free blocks
        for (freeList = 2 + ((totalInodes - 1) * INODE_SIZE / Disk.blockSize)
             ;freeList < totalBlocks; ++freeList)
        {
            // Reset all data in the block
            info = new byte[Disk.blockSize];
            for (int i = 0; i < Disk.blockSize; ++i)
            {
                info[i] = 0;
            }

            // Write pointer to next block
            SysLib.int2bytes(freeList + 1, info, 0);

            // Finish formatting
            SysLib.rawwrite(freeList, info);
        }

        sync();

		System.out.println("Finished Formatting");
    }

    // Update the variables in super block
    public void sync()
    {
        byte[] info = new byte[Disk.blockSize];

        // Update the variables
        SysLib.int2bytes(totalBlocks, info, 0);
        SysLib.int2bytes(totalInodes, info, 4);
        SysLib.int2bytes(freeList, info, 8);

        // Write back to super block
        SysLib.rawwrite(0, info);
    }

    public int getFreeBlock()
    {
        int n = freeList;
        if (n != -1)
        {
            byte[] info = new byte[Disk.blockSize];
            SysLib.rawread(n, info);

            // Read the pointer
            freeList = SysLib.bytes2int(info, 0);


            SysLib.int2bytes(0, info, 0);
            SysLib.rawwrite(n, info);
        }
        return n;
    }

    public boolean returnBlock(int n)
    {
        if (n < 0) return false;

        byte[] info = new byte[Disk.blockSize];

        // Initialize block
        for (int i = 0; i < Disk.blockSize; ++i)
        {
            info[i] = 0;
        }

        SysLib.int2bytes(freeList, info, 0);
        SysLib.rawwrite(n, info);
        freeList = n;
        return true;
    }
}