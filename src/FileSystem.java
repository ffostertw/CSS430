import java.io.File;

public class FileSystem{
    private SuperBlock superblock;
    private Directory directory;
    private FileStructureTable filetable;

    public FileSystem(int diskblocks){
        superblock = new SuperBlock(diskblocks);

        directory = new Directory (superblock.inodeBlocks);

        filetable = new FileStructureTable(directory);

        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if(dirSize > 0){
            byte[] dirData = new byte [dirSize];
            read (dirEnt, dirData);
            directory.bytes2directory(dirData);

        }
        close(dirEnt);
    }

    void sync(){
        FileTableEntry dirEnt = open("/", "w");
        byte[] data  = directory.directory2bytes();
        write(dirEnt, data);
        close(dirEnt);

        superblock.sync();
    }

    boolean format(int files){
        if(!filetable.fempty()){
            return false;
        }else{
            superblock.format(files);
            directory = new Directory(superblock.totalBlocks);
            filetable = new FileStructureTable(directory);
            return true;
        }
    }

    FileTableEntry open (String fileName, String mode){
        if(fileName == null || mode == null ||
                fileName == "" || mode == ""){
            return null;
        }
        FileTableEntry ent = filetable.falloc(fileName, mode);
        if(mode =="w"){
            if(!deadllocAllBlocks(ent)){
                return null;
            }
        }

        return ent;
    }

    boolean close (FileTableEntry ftEnt){
        synchronized (ftEnt){
            ftEnt.count --;
            if(ftEnt.count > 0){
                return true;
            }
        }
        try{
            filetable.ffree(ftEnt);
        }catch(Exception e){
            return false;
        }
        return false;
    }

    int fsize (FileTableEntry ftEnt){
        return ftEnt.inode.length;
    }
    int read(FileTableEntry ftEnt, byte[] buffer){
        if(ftEnt == null ||
                ftEnt.mode =="a" ||
                ftEnt.mode == "w"){
            return -1;

        }
        byte[] data = new byte[Disk.blockSize];
        int length = buffer.length;
        int count = 0;
        synchronized (buffer){
            while(ftEnt.seekPtr < fsize(ftEnt)
                    &&length > 0 ){
                SysLib.rawread(ftEnt.inode.findBlock(ftEnt.seekPtr), data);
                int info = ftEnt.seekPtr % Disk.blockSize;
                int move = Math.min(Math.min(Disk.blockSize - info, length),
                        fsize(ftEnt) - ftEnt.seekPtr);

                System.arraycopy(data, info, buffer, count, move);

                ftEnt.seekPtr += move;
                length -= move;
                count += move;
            }
            return count;
        }
    }

    int write (FileTableEntry ftEnt, byte[] buffer){
        if(ftEnt == null || ftEnt.mode == "r" || ftEnt.inode == null){
            return -1;
        }
        synchronized (ftEnt){
            int length = buffer.length;
            int count = 0;
            while(length > 0){
                int bnum = ftEnt.inode.findBlock(ftEnt.seekPtr);
                short freeb = (short)superblock.getFreeBlock();
                if(bnum == -1){
                    int check = ftEnt.inode.setTargetBlockUsed(freeb,ftEnt.seekPtr);

                    if(check == -1){
                        return -1;
                    }
                    else if(check == -2){
                        short freeb2 = (short)superblock.getFreeBlock();
                        if(ftEnt.inode.setTargetBlockUsed(freeb,ftEnt.seekPtr) !=0 ){
                            return -1;
                        }
                        else if(!ftEnt.inode.setIndexBlockUsed(freeb2)){
                            return -1;
                        }
                    }

                    bnum = freeb;
                }

                byte[] data = new byte[Disk.blockSize];
                int offset = ftEnt.seekPtr % Disk.blockSize;
                int move = Math.min(Disk.blockSize - offset, length);
                System.arraycopy(buffer, count, data, offset, move);
                SysLib.rawwrite(bnum, data);
                count += move;
                ftEnt.seekPtr += move;
                length -= move;

                if(ftEnt.inode.length < ftEnt.seekPtr){
                    ftEnt.inode.length = ftEnt.seekPtr;
                }

            }

            ftEnt.inode.toDisk(ftEnt.iNum);
            return count;
        }
    }

    //deallocate inodes and blocks
    private boolean deadllocAllBlocks (FileTableEntry ftEnt){
        if(ftEnt.inode == null || ftEnt.inode.count >1){
            return false;
        }

        //deallocate direct blocks
        for(int i = 0; i< 11; i++){
            if(ftEnt.inode.direct[i] != -1){
                superblock.returnBlock(ftEnt.inode.direct[i]);
            }
            ftEnt.inode.direct[i] = -1;

        }
        //deallocate indirect blocks
        byte[] dataneedclean = ftEnt.inode.delIndexBlock();
        if(dataneedclean == null){
            ftEnt.inode.toDisk(ftEnt.iNum);
            return true;
        }else{
            int bnum = SysLib.bytes2short(dataneedclean, 0);
            while(bnum != -1){
                superblock.returnBlock(bnum);
                bnum = SysLib.bytes2short(dataneedclean, 0);
            }
            ftEnt.inode.toDisk(ftEnt.iNum);
            return true;
        }
    }

    boolean delete (String filename){
        if(directory.namei(filename) < 0){
            return false;
        }
        return directory.ifree(directory.namei(filename));
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek(FileTableEntry ftEnt, int offset, int whence){
        if(ftEnt == null){
            return -1;
        }
        int seekp, EOF;

        if(whence > 2 || whence < 0){
            return -1;
        }
        else{
            seekp = ftEnt.seekPtr;
            EOF = fsize(ftEnt);

            if(whence == SEEK_SET){
                seekp = offset;
                ftEnt.seekPtr = seekp;

            }else if(whence == SEEK_CUR){
                seekp+= offset;
                ftEnt.seekPtr = seekp;

            }else if(whence == SEEK_END){
                seekp = EOF + offset;
                ftEnt.seekPtr = seekp;

            }
            if(seekp < 0){
                seekp = 0;
                ftEnt.seekPtr = 0;

            }else if(seekp > fsize(ftEnt)){
                seekp = EOF;
                ftEnt.seekPtr = EOF;

            }
        }
        return seekp;
    }
}

