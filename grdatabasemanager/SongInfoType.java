package grdatabasemanager;

public enum SongInfoType {
    FILENAME(1), ARIST(2), TITLE(3), GENRE(4), FILETYPE(5);
    
    private final int value;
    
    private SongInfoType(int value){
        this.value = value;
    }
    
    public int getValue(){
        return this.value;
    }
}
