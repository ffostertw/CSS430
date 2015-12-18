
public class Directory{

    private static int maxChars = 30;
    private int fsize[];
    private char fnames[][];


    public Directory (int maxInumber){

        fsize = new int [maxInumber];
        for (int i = 0; i< maxInumber; i++){
            fsize[0] = 0;
            fnames = new char [maxInumber][maxChars];
            String root = "/";
            fsize[0] = root.length();
            root.getChars(0, fsize[0], fnames[0], 0);
        }
    }

    //initial the directory with given data
    public void bytes2directory(byte data[]) {
        int info  = 0;
        for(int i = 0; i < fsize.length ; i++){
            fsize[i] = SysLib.bytes2int(data, info);
            info += 4;
        }
        for(int x = 0; x< fnames.length; x++){
            String n = new String(data, info, maxChars+maxChars);
            info += maxChars*2;
            if(! (fsize[x] > maxChars)){
                n.getChars(0, fsize[x], fnames[x], 0);
            }

        }
    }


    //convert directory into byte array
    public byte[] directory2bytes(){
        byte[]a = new byte[0];
        int info = 0;
        for(int i = 0; i < fsize.length ; i ++){
            SysLib.int2bytes(fsize[i], a, info);
            info = info + 2;
        }
        for(int i = 0; i < fnames.length; i++){
            String n = new String(fnames[i], 0, fsize[i]);
            System.arraycopy(n.getBytes(), 0, a, info, n.getBytes().length);
            info = info + maxChars *2;
        }
        return a;
    }

    // allocate a new inode
    public short ialloc (String fileName){
        int x = 0;
        while (x < fsize.length){
            if(fsize[x] == 0){
                if(fileName.length() > maxChars){
                    fsize[x] = maxChars;
                }else{
                    fsize[x] = fileName.length();
                }

                fileName.getChars(0, fsize[x], fnames[x], 0);
                return (short)x;
            }
            x++;
        }
        return -1;
    }

    // deallocate the inumber
    public boolean ifree (short iNumber){
        if(fsize[iNumber]<= 0) {
            return false;
        }else{
            fsize[iNumber] = 0;
            return true;
        }
    }

    //return the inumber of the given file
    public short namei (String fileName){
        int x = 0;
        while (x < fsize.length){
            String n = new String(fnames[x], 0, fsize[x]);
            if(fileName.equalsIgnoreCase(n)){
                return (short)x;
            }
            x++;
        }
        return -1;
    }
}