// not sure about the flag status......
import java.util.Vector;

public class FileStructureTable{
    private Vector<FileTableEntry> table;
    private Directory dir;

    public FileStructureTable(Directory dirr){
        table = new Vector<FileTableEntry>();
        dir = dirr;
    }

    //allocate a new file structure table entry for the file name,
    // allocate/retrieve and register the corresponding inode using dir
    // increase inode count
    // return reference to this file structure table entry
    public synchronized FileTableEntry falloc (String fileName, String mode){
        Inode inode = null;
        short iNum;

        if(fileName.equals("/")){
            iNum = 0;
        }else{
            iNum = dir.namei(fileName);
        }


        if(iNum < 0){  // file doesn't exist
            if(mode.equals("r")){
                return null;
            }
            else{
                iNum = dir.ialloc(fileName);
                inode = new Inode();
                inode.flag = 2;
            }
        }else{
            inode = new Inode(iNum);
            if(mode.equals("r")) {
                if (inode.flag == 0) {
                    inode.flag = 1;
                }
            }else{
                if(inode.flag == 4){
                    return null;
                }
            }try{
                wait();
            }catch (Exception e){

            }
        }

        inode.count ++;
        inode.toDisk(iNum);
        FileTableEntry newEntry = new FileTableEntry(inode, iNum, mode);
        table.add(newEntry);
        return newEntry;

    }

    //receive a file table entry reference, save the corresponding inode to the disk
    // free this file table entry, return true if this file table found in my table
    public synchronized  boolean ffree (FileTableEntry e){
        if(!table.removeElement(e)){
            return false;
        }
        else{
            Inode del = e.inode;
            del.count --;
            if(del.flag == 1){
                del.flag = 0;
            }else if(del.flag == 2 || del.flag == 3){
                notify();
            }
            del.toDisk(e.iNum);
            return true;
        }
    }

    public synchronized  boolean fempty(){
        return table.isEmpty();
    }
}
