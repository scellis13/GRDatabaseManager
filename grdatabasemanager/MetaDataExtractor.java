package grdatabasemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.text.DecimalFormat;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class MetaDataExtractor {
    //Private Instance Variables
    private final File songFile; 
    private File parentFolder;
    private String fileName, filePath, parentPath, fileType;  
    //TikaApp global variables
    private String artistTika, titleTika, genreTika;
    private double fileSize;
    private Time songDuration;
    private Metadata tikaData;
    private FileInputStream tikaStream;
    //Method Extraction Status'
    private boolean fileExtractStatus, tikaExtractStatus;
    private static int TOTAL_TIKA;
    
    /** MetaDataExtractor Constructor
     *      The Class constructor calls three private class methods:
     *          extractTagInfo(), extractFileInfo(), extractTikaInfo().
     *      Each method will set its own boolean value if all data extractions
     *      were complete.
     *  @param file of Object Type (File) is required to create a 
     *      MetaDataExtractor object.
     */
    public MetaDataExtractor(File file){
        this.songFile = file;
        extractFileInfo();
        extractTikaInfo();
    }
    
    /** File Class Extraction Method
     *      This method uses the Standard Java IO Library to extract the
     *      following:
     *          1. String   -   fileName
     *          2. String   -   filePath
     *          3. String   -   parentPath
     *          4. File     -   parentFolder
     *          5. Double   -   fileSize
     */
    private void extractFileInfo(){
        this.fileName = songFile.getName();
        this.filePath = songFile.getPath();
        this.parentPath = songFile.getParent();
        this.parentFolder = songFile.getParentFile();
        
        //Double size
        Long sizeLong = songFile.length();
        double sizeDouble = (sizeLong.doubleValue() / 1.049e6);
        DecimalFormat format = new DecimalFormat("#.##");
        format.setRoundingMode(RoundingMode.FLOOR);
        this.fileSize = Double.parseDouble(format.format(sizeDouble));
        
        fileExtractStatus = true;
    }
    
    /** Tika Apache Extraction Method
     *      This Method uses the Tika Apache Metadata Library to extract the
     *      following:
     *          1. String   -   fileType
     *          2. Time     -   songDuration
     *      A FileInputStream is opened within the try block, and closed within 
     *      the finally block.
     */
    private void extractTikaInfo(){
        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            tikaData = new Metadata();
            tikaStream = new FileInputStream(this.songFile);
            ParseContext context = new ParseContext();
            parser.parse(tikaStream, handler, tikaData, context);
            
            //Extracts File Type
            this.fileType = tikaData.get("Content-Type");
            this.artistTika = tikaData.get("xmpDM:artist");
            this.titleTika = tikaData.get("title");
            this.genreTika = tikaData.get("xmpDM:genre");
            //Extracts Song Duration
            try {
                String durStr = tikaData.get("xmpDM:duration");
                double durDouble = Double.parseDouble(durStr);
                long decimal = (long) durDouble;
                songDuration = new Time(decimal);
                songDuration.setHours(0);
            } catch (NullPointerException e){
                songDuration = new Time(0);
            }
            
            tikaExtractStatus = true;
            TOTAL_TIKA++;
        } catch (IOException | SAXException | TikaException e){
            fileExtractStatus = false;
            tikaExtractStatus = false;
        } finally {
            try {
                tikaStream.close();
            } catch (IOException e){}
        }
    }
    
    /** Getter Methods for the following Class Variables:
     *      String      - fileName, songArtist, songTitle, genreList, filePath,
     *                    parentPath, fileType
     *      File        - songFile, parentFolder
     *      Double      - fileSize
     *      Time        - songDuration
     *      Boolean     - fileExtractStatus, tagExtractStatus, tikaExtractStatus
     *      ID3v2_3     - tag
     *      Metadata    - tikaData
     *      int         - TOTAL_ID3v1, TOTAL_ID3v2, TOTAL_TIKA
     */ 
    public String getFileName(){return this.fileName;}
    public String getTikaArtist(){return this.artistTika;}
    public String getTikaTitle(){return this.titleTika;}
    public String getTikaGenre(){return this.genreTika;}
    public String getFilePath(){return this.filePath;}
    public String getParentPath(){return this.parentPath;}
    public String getFileType(){return this.fileType;}
    public File getSongFile(){return this.songFile;}
    public File getParentFolder(){return this.parentFolder;}
    public Double getFileSize(){return this.fileSize;}
    public Time getSongDuration(){return this.songDuration;}
    public boolean getFileExtractStatus(){return this.fileExtractStatus;}
    public boolean getTikaExtractStatus(){return this.tikaExtractStatus;}
    public Metadata getTikaMetadata(){return this.tikaData;}
    public static int getTotalTika(){return MetaDataExtractor.TOTAL_TIKA;}
    
    /** Setter Methods for the following Metadata:
     *      fileName, songArtist, songTitle, genreList
     *  
     *      Each method will use the Java ID3 Stream to create or overwrite the
     *      Metadata information. However, the File Name will be changed using
     *      the built-in Java File IO Library.
     */
    public boolean setFileName(String newFileName){
        assert this.fileType.equalsIgnoreCase("audio/mpeg");
        
        
        int index = newFileName.lastIndexOf('.');
        if(index > 0 && newFileName.substring(index+1).toLowerCase().equals("mp3")
                && !this.fileName.equals(newFileName)){

            try { 
                Path origPath = Paths.get(getFilePath());
                Path destinationPath = Paths.get(getParentPath() + "\\" + newFileName);
                Files.move(origPath, destinationPath);
                setFileNameHelper(newFileName);
                
                return true;
            } catch (IOException e){
                return false;
            }
        } else {
            return false;
        }
        
    }
    
    private void setFileNameHelper(String newFileName){
        this.fileName = newFileName;
    }
    
    @Override
    public String toString(){
        return "Filename: " + getFileName() +
                "\nArtist: " + getTikaArtist() +
                "\nTitle: " + getTikaTitle() +
                "\nGenre(s): " + getTikaGenre() +
                "\nSize: " + getFileSize() + " Megabytes" + 
                "\nDuration: " + getSongDuration() +
                "\nFile Type: " + getFileType()
                ;
    }
    
    public String tikaString(){
        return "Filename: " + getFileName() +
                "\nArtist: " + getTikaArtist() +
                "\nTitle: " + getTikaTitle() +
                "\nGenre(s): " + getTikaGenre()
                ;
    }
}
