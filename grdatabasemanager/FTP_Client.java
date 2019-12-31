
package grdatabasemanager;

import java.io.*;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.io.CopyStreamAdapter;

public class FTP_Client {
    private String host, user, pass;
    private int port;
    private boolean connection, withPort;
    private FTPClient ftp;
    private File fileToUpload;
    private boolean fileUploadNew, fileUploadMedia;
    private int percent;
    private InputStream inputStream;
    private BufferedInputStream bufferedStream;
    
    public boolean connect(String host, String user, String pass){
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.withPort = false;
        
        connectHelper(false);

        return this.connection;
    }
    
    public boolean connect(String host, int port, String user, String pass){
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.port = port;
        this.withPort = true;
        
        connectHelper(true);
        
        return this.connection;
    }
    
    public void disconnect(){
        try {
            if(ftp.isConnected()){
                ftp.logout();
                ftp.disconnect();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    private void connectHelper(boolean withPort){
        this.connection = false;
        ftp = new FTPClient();
        try {
            if(withPort){
                ftp.connect(host, port);
            } else {
                ftp.connect(host);
            }
            ftp.login(user, pass);
            int reply = ftp.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)){
                ftp.disconnect();
                
            }
            ftp.enterLocalPassiveMode();
            ftp.changeWorkingDirectory("\\media\\new\\");
            this.connection = true;
            
        } catch (IOException e){
            System.out.println("FTP_Client: Connection Failed.");
        }

    }
    
    public boolean uploadFile(File f){
        this.fileToUpload = f;
        checkConnection();
        
        fileUploadNew = false;
        
        try {
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.setBufferSize(8192);
            String remoteFile = fileToUpload.getName();
            
            inputStream = new FileInputStream(fileToUpload);
            
            ftp.storeFile(remoteFile, inputStream);
            fileUploadNew = true;
        } catch (IOException e){
            System.out.println("\tFTP_Client: New Upload Fail," + e.getMessage());
            System.out.println("");
            System.out.println(ftp.getReplyCode());
            System.out.println(ftp.getReplyString());
            System.out.println("");
        } finally {
            try{
                inputStream.close();
            } catch (IOException e){};
        }
        return fileUploadNew;
    }
    
    public FTPClient getFTP(){
        return this.ftp;
    }
    
    private void checkConnection(){
        if(!ftp.isConnected()){
            connectHelper(this.withPort);
        }
    }
    
    
    /*
        EXAMPLE SESSION
    
    public static void main(String[] args){
        
        String host = "host-ip-address";
        String user = "host-username";
        String pass = "host-password";
        FTP_Client client = new FTP_Client();
        System.out.println("Connected: " + client.connect(host, user, pass));
        
        File dir = new File("D:\\Documents\\Google Onedrive Sync\\Cloud Documents\\GotRadio\\FTP_Client\\src\\resources\\test_files");
        
        for(File f : dir.listFiles()){
            if(f.isFile()){
                client.uploadFile(f);
            }
        }
        
        client.disconnect();
    }
    */
}
