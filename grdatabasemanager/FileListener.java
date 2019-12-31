/**
 * Last Updated On: 11/19/2019
 * IDE Computer: MSI Laptop
 */
package grdatabasemanager;

import java.io.*;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;
import javafx.collections.*;


public class FileListener {
    
    private Path dirPath;
    private WatchService watcher;
    private WatchKey watchKey = null;
    private boolean watching = false, filesInFolder = false;
    private int filesFound = 0;
    private Thread watcherThread;
    private ObservableList<String> fileList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    
    public FileListener(File strPath, ObservableList<String> fileList){

            this.dirPath = Paths.get(strPath.getPath());
            this.fileList = fileList;
            
    }
    
    public void startService() {
        /*
            watcherThread = new Thread(){

                @Override
                public void run(){
                    startServiceHelper();
                }

            };
            watcherThread.setDaemon(true);
            watcherThread.start();
        */
        startServiceHelper();
    }
    
    private void startServiceHelper(){
        watching = true;
        System.out.println("Starting Watcher Service...");
        try{
            watcher = dirPath.getFileSystem().newWatchService();
            dirPath.register(watcher,   StandardWatchEventKinds.ENTRY_CREATE, 
                                        StandardWatchEventKinds.ENTRY_DELETE, 
                                        StandardWatchEventKinds.ENTRY_MODIFY);
            
            while(watching){
                filesInFolder = false;
                watchKey = watcher.take();
                
                if(watchKey != null){
                    watchKey.pollEvents().stream().forEach(event -> {
                        WatchEvent.Kind kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path file = ev.context();
                        if(kind == ENTRY_CREATE) {  
                            fileList.add(file.getFileName().toString());
                            System.out.println("Watcher Service Found: " + file.getFileName().toString());
                        } else if(kind == ENTRY_DELETE){
                            fileList.remove(file.getFileName().toString());
                        }
                    });

                }

                watchKey.reset();
            }
        } catch (InterruptedException e){
            System.out.println(e.getMessage());
            System.out.println("Ending Watcher Service in Catch Clause...");
            System.out.println("Please call startService() again...");
            watchKey.reset();
            watching = false;
            try{
                watcher.close();
            } catch (IOException ex){};
        } catch (IOException io){
            System.out.println(io.getMessage());
        } catch (ConcurrentModificationException exe){
            System.out.println(exe.getMessage());
        }
        
    }
    
    public void stopService(){
        this.watching = false;
        try{
            watchKey.pollEvents().clear();
            watchKey.reset();
            watcher.close();
            System.out.println("Stopping Watcher Service...");
        } catch (IOException | NullPointerException e){};
        
    }
    
    public void setWatchingStatus(boolean status){
        this.watching = status;
    }
    
    public boolean getWatchingStatus(){
        return this.watching;
    }
    
    public ObservableList<String> getObservableList(){
        return this.fileList;
    }
}
