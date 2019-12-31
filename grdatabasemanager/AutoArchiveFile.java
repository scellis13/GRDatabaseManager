package grdatabasemanager;

public class AutoArchiveFile implements Comparable<AutoArchiveFile>{
    private java.sql.Timestamp timeFileProcessed;
    private final String fileName;
    private ProgressEnum archiveStatus, uploadStatus;
    private String errorMessage;
    
    public AutoArchiveFile(java.sql.Timestamp time, String name){
        this.timeFileProcessed = time;
        this.fileName = name;
        this.archiveStatus = ProgressEnum.WAITING;
        this.uploadStatus = ProgressEnum.WAITING;
        this.errorMessage = "";
    }
    
    public AutoArchiveFile(java.sql.Timestamp time, String name, ProgressEnum archiveStatus, ProgressEnum uploadStatus){
        this.timeFileProcessed = time;
        this.fileName = name;
        this.archiveStatus = archiveStatus;
        this.uploadStatus = uploadStatus;
        this.errorMessage = "";
    }
    
    public void setTime(java.sql.Timestamp time){
        this.timeFileProcessed = time;
    }
    
    public void setArchiveStatus(ProgressEnum status){
        this.archiveStatus = status;
    }
    
    public void setUploadStatus(ProgressEnum status){
        this.uploadStatus = status;
    }
    
    public void setErrorMessage(String message){
        this.errorMessage = message;
    }
    
    public String getFileName(){
        return this.fileName;
    }
    
    public java.sql.Timestamp getTimeFileProcessed(){
        return this.timeFileProcessed;
    }
    
    public ProgressEnum getArchiveStatus(){
        return this.archiveStatus;
    }
    
    public ProgressEnum getUploadStatus(){
        return this.uploadStatus;
    }
    
    public String getErrorMessage(){
        return this.errorMessage;
    }
    
    @Override
    public String toString(){
        return  "'" + fileName + "' last processed at '" + timeFileProcessed 
                    + "', Archive Status: " + archiveStatus + ", Upload Status: " 
                    + uploadStatus;
    }
    
    @Override
    public int compareTo(AutoArchiveFile otherFile){
        return timeFileProcessed.compareTo(otherFile.getTimeFileProcessed());
    }
    
    public enum ProgressEnum {
        IN_PROGRESS, WAITING, FAILED, SUCCEEDED;
    }
}


