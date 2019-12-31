package grdatabasemanager;

import java.util.*;

public class SongValidation {
    
    private MetaDataExtractor song;
    private String fileName, artist, title, genreList, fileType;
    private ArrayList<ErrorType> errors;
    
    public SongValidation(MetaDataExtractor song){
        this.song = song;
        this.fileName = song.getFileName();
        this.artist = song.getTikaArtist();
        this.title = song.getTikaTitle();
        this.genreList = song.getTikaGenre();
        this.fileType = song.getFileType();
        errors = new ArrayList<>();
        checkValidation();
    }
    
    private void checkValidation(){
        
        //First Check the File is an MP3 File
        if(!fileName.substring(fileName.lastIndexOf(".")+1).equals("mp3")){
            errors.add(ErrorType.valueOf(1));
        }
        
        //Second Check Tika pulled audio/mp3
        if(fileType != null){
            if(!fileType.equals("audio/mpeg")){
                errors.add(ErrorType.valueOf(2));
            }
        } else {
            errors.add(ErrorType.valueOf(2));
        }
        
        //Third Check If Artist, Title, and Genre are present
        if(artist != null){
            if(artist.equals("")){
            errors.add(ErrorType.valueOf(3));
            }
        } else {
            errors.add(ErrorType.valueOf(3));
        }
        
        if(title != null){
            if(title.equals("")){
            errors.add(ErrorType.valueOf(4));
            }
        } else {
            errors.add(ErrorType.valueOf(4));
        }
        
        if(genreList != null){
            if(genreList.equals("")){
            errors.add(ErrorType.valueOf(5));
            }
        } else {
            errors.add(ErrorType.valueOf(5));
        }
        //End of Third Check: Checking Individual Tags
        
        //Start of Fourth and Fifth Checks
            //If-Else Skips Checking Filename Mismatch and Invalid Genre
            //If any of the Tags are null
        if(artist == null || title == null || genreList == null){
            errors.add(ErrorType.valueOf(6));
            errors.add(ErrorType.valueOf(7));
        } else {
            
            //Fourth Check If FileName matches Artist - Title.mp3
            if(!fileName.equalsIgnoreCase(artist + " - " + title + ".mp3")){
                errors.add(ErrorType.valueOf(6));
            }
            
            //Fifth Check If Genre is Valid
            if(genreList.contains(",")){
                String[] genreArr = genreList.split(",");
                for(int i = 0; i < genreArr.length; i++){
                    if(!GENRES.contains(genreArr[i].trim().toLowerCase())){
                        errors.add(ErrorType.valueOf(7));
                    }
                }
            } else { //Single Genre, check without delimiter
                if(!GENRES.contains(genreList.trim().toLowerCase())){
                    errors.add(ErrorType.valueOf(7));
                }
            }
        }                                              
    }                     
    public boolean hasErrors(){
        return !errors.isEmpty();
    }
    
    public ArrayList<ErrorType> getErrorList(){
        return errors;
    }
    
    public enum ErrorType {
        Missing_MP3_Extension(1),
        Incorrect_File_Type(2),
        Missing_Artist_Tag(3),
        Missing_Title_Tag(4),
        Missing_Genre_Tag(5),
        Filename_Tag_Mismatch(6),
        Incorrect_Genre_Tag(7);
    
        private int value;
        private static Map map = new HashMap<>();
        
        private ErrorType(int value){
            this.value = value;
        }
        
        static {
            for (ErrorType error : ErrorType.values()){
                map.put(error.value, error);
            }
        }
        
        public static ErrorType valueOf(int error){
            return (ErrorType) map.get(error);
        }
        
        public int getValue(){
            return value;
        }
    }
}
