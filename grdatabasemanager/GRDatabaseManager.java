package grdatabasemanager;
/*  UPDATES 
    Last Updated: 12/20/2019
    Updated Source: Sean Laptop
        Version 1.6
            SSH_SQL_Test.java
                - Added new SSH and SQL Connection for Server connecting.
            DatabaseConnect.java
                - Added new java file to connect to SSH java file.
        Version 1.5
            FileListener.java
                - Added File Listener file/capabilities
            AutoArchive Methods
                - Added several new AutoArchive methods for Settings and DisplayWindow
            ExitProgramHelper
                - Added Alert Confirmation for Normal Exiting.
        Version 1.4
            leftCornerInfoPane()
                - Added new Server Connection Test to prevent user from archiving
                    without the Server Connection established.
            FTP_Client.java
                - Fixed exception catch for invalid information.
        Version 1.3
            **New Pane, leftCornerInfoPane()
                - Added VBox with inner HBox Panes to house Connection Status
                    and Last Archived Time
            **New Operation, serverUpload()
                - New upload sends valid songs to Server's New Folder, if connection or
                    upload fails, song will be copied into local archived failed upload.
        Version 1.2
            folderChooser() and DEFAULT selected Folder:
                - Added NullPointerException to folderChooser() in case user
                    exits Directory Chooser.
                - Set Default Selected Folder to "C:\\Users\\vstar\\Dropbox\\programmer_share\\upload_archive"
            uploadData():
                - Added new Data Input for 'Time Added' into Database
                - Rearranged PreparedStatement Data to match MySQL Columns
            Added new Method: checkDatabaseConnection();
                - Checks whether the SQL Connection is still established, if not, 
                    the database will automatically connect before doing any SQL 
                    Operation.
            Removed Character Exception during SongValidation for Artist and Title
        Version 1.1
        - New Global Final Variable
                Added 'File archiveFolder' for constant final archive location.
        - uploadData() Method
                Changed MySQL statement from insert to replace
        - startArchive() Method
                Added Operation to move Archived Files from source to Archive 
                Folder
*/
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javafx.application.*;
import javafx.collections.*;
import javafx.concurrent.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;

public class GRDatabaseManager extends Application {
    //Finals and Constants
    private DatabaseConnect databaseConnect;
    private static final double versionNumber = 1.6;
    private static final int TILE_SIZE = 40;
    private static final Font ITALIC = Font.font(
            "Times New Roman",
            FontPosture.ITALIC,
            20);
    private static final int numCols = 24;
    private static final int numRows = 12;
    private static final ImageView imv = new ImageView();
    private static final Image img = new Image(GRDatabaseManager.class.getResourceAsStream("/resources/images/GR_Logo.png"));
    private static final HBox picturePane = new HBox();
    private static final File archiveFolder = new File("D:\\Music Library Clean\\Archived"); //Real Archive Folder
    //private static final File archiveFolder = new File("C:\\Users\\scell\\Documents\\Google Drive Sync\\Cloud Documents\\GotRadio\\Song Downloads\\Archived"); //Test Folder on Laptop Machine
    private File folderSelected = new File("C:\\Users\\vstar\\Dropbox\\programmer_share\\upload_archive"); //Real folderSelected
    //private File folderSelected = new File("C:\\Users\\scell\\Documents\\Google Drive Sync\\Cloud Documents\\GotRadio\\Song Downloads"); //Test folderSelected on Laptop
    private File autoArchiveFolderSelected = folderSelected, autoArchiveFolderSelectedTemp = autoArchiveFolderSelected;
    //Private Class Variables
    private MenuBar menu;
    private Stage stage;
    private Scene scene;
    private GridPane gPane;
    private Button  folderButton, startButton, pauseButton, cancelButton, 
                    popUpWindowButton, popUpWindowButton2, autoArchiveSaveButton;
    private Label folderLabel, infoLabel, statusLabel, currentSessionLabel, 
                totalSessionLabel, dbStatusLabel, dbIndicatorLabel, lastUpdatedLabel, 
                lastUpdatedIndicatorLabel, uploadLabel, uploadStatus, totalGoodCountLabel, 
                totalBadCountLabel, pendingCountLabel, autoArchiveLabel, autoArchiveFolderTempLabel;
    private ProgressBar pb;
    private ProgressIndicator pi;
    private Task archiveWorker, uploadTasker;
    private Thread taskThread, uploadThread;
    private int i, goodSongs, badSongs, totalGoodSongs = 0, totalBadSongs = 0, totalPendingUploads = 0, totalGoodUploads = 0, totalBadUploads;
    private String error;
    private ArrayList<String> goodList = new ArrayList<String>();
    private ArrayList<String> badList = new ArrayList<String>();
    private ListView<String> goodListView = new ListView<String>(); 
    private ListView<String> badListView = new ListView<String>();
    private ArrayList<String> resultGoodUploadList = new ArrayList<String>();
    private ArrayList<String> resultBadUploadList = new ArrayList<String>();
    private ArrayList<String> pendingUploadList = new ArrayList<String>();
    private ListView<String> resultGoodUploadView = new ListView<String>(); 
    private ListView<String> resultBadUploadView = new ListView<String>(); 
    private ListView<String> pendingUploadView = new ListView<String>();
    private Connection myConn;
    private Connection serverConn;
    private ResultSet result;
    private PreparedStatement statement;
    private static final Image applicationIcon = new Image(GRDatabaseManager.class.getResourceAsStream("/resources/images/gotradio_upload_icon.jpg"));
    private Calendar calendar = Calendar.getInstance();
    private java.sql.Timestamp lastArchived;
    private boolean archiveRunning;
    private FTP_Client client;
    //File Listener variables
    private ObservableList<String> fileList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private File dirListener = new File("D:\\Music Library Clean");
    private boolean autoArchiveOn = false, autoArchiveRunning = false;
    boolean dbConnection = false, transferConnection = false, serverDatabaseConnection = false;
    private CheckBox autoArchiveCheckBox;
    private Label   autoArchiveSaveButtonLabel, autoArchiveStatusLabel, autoArchiveStatus, 
                    autoArchiveProgress, autoArchiveProgressLabel;
    private final String AUTO_ARCHIVE_SETTINGS_STYLE = "-fx-background-color: gainsboro; -fx-font-size: 16;";
    private Stage autoArchiveSettingsStage = new Stage();
    private Task autoArchiveTask;
    private Thread autoArchiveThread;
    private FileListener listener;
    
    //Auto Archive Display Global Variables
    private TableView autoArchiveTableView = new TableView();
    private TableColumn<AutoArchiveFile, java.sql.Timestamp> dateTimeCol;
    private int autoArchiveCount = 0, autoArchiveSuccessCount = 0, autoArchiveFailCount = 0, 
                autoArchiveUploadSuccessCount = 0, autoArchiveUploadFailCount = 0;
    private ObservableList<AutoArchiveFile> autoArchiveFileList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private Label   listenerStatus = new Label(), listenerFolder = new Label(), listenerCurrentProcess = new Label(), listenerCurrentFile = new Label(), 
                    totalAutoArchiveCount = new Label(), totalAutoArchiveResultSuccessCount = new Label(), 
                    totalAutoArchiveResultFailCount = new Label(), totalAutoArchiveUploadResultSuccessCount = new Label(),
                    totalAutoArchiveUploadResultFailCount = new Label(), totalProcessedLabel, totalProcessedCount;
    private String currentProcess = "Not Active.", currentAutoFile = "", autoArchiveError = "";
    
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        
        //Create GridPane Layout
        gPane = new GridPane();
            //gPane.setGridLinesVisible(true);
            gPane.setStyle("-fx-background-color: #FFFFFF;");
            
            for(int i = 0; i < numCols; i++){
                ColumnConstraints colConst = new ColumnConstraints();
                colConst.setPercentWidth(100.0 / numCols);
                gPane.getColumnConstraints().add(colConst);
            }

            for(int i = 0; i < numRows; i++){
                RowConstraints rowConst = new RowConstraints();
                rowConst.setPercentHeight(100.0 / numRows);
                gPane.getRowConstraints().add(rowConst);
            }
        //End of GridPane Creation
        
        //Add GotRadio Image Logo    
        imv.setImage(img);
        imv.setFitHeight(4*TILE_SIZE);
        imv.setFitWidth(4*TILE_SIZE);
        picturePane.getChildren().add(imv);
        
        //Create MenuBar
        menu = new MenuBar();
        Menu fileMenu = new Menu("File");
            MenuItem exit = new MenuItem("Exit");
            exit.setOnAction(this::exitProgram);
        fileMenu.getItems().addAll(exit);
        Menu autoArchiveMenu = new Menu("Auto Archive");
            MenuItem autoArchiveSettings = new MenuItem("Settings");
                autoArchiveSettings.setOnAction(this::autoArchiveSettingsWindow);
            MenuItem autoArchiveWindow = new MenuItem("Display Window");
                autoArchiveWindow.setOnAction(this::autoArchiveDisplayWindow);
        autoArchiveMenu.getItems().addAll(autoArchiveWindow, autoArchiveSettings);
        Menu helpMenu = new Menu("Help");
        menu.getMenus().addAll(fileMenu, autoArchiveMenu, helpMenu);
        menu.setMinSize(24*TILE_SIZE, .5*TILE_SIZE);
        //End of MenuBar
        
        //leftCornerInfoPane Start
        VBox leftCornerInfoPane = new VBox();
        leftCornerInfoPane.setPadding(new Insets(10));
        leftCornerInfoPane.setSpacing(0);
        
            HBox dbStatusPane = new HBox();
            dbStatusPane.setSpacing(10);
            dbStatusLabel = new Label("Database/Server Connection: ");
            dbStatusLabel.setFont(new Font("Times New Roman", 12));
            dbIndicatorLabel = new Label("Status");
            dbIndicatorLabel.setFont(new Font("Times New Roman", 12));
            
            HBox lastUpdatedPane = new HBox();
            lastUpdatedPane.setSpacing(10);
            lastUpdatedLabel = new Label("Last Archived: ");
            lastUpdatedLabel.setFont(new Font("Times New Roman", 12));
            lastUpdatedIndicatorLabel = new Label();
            lastUpdatedIndicatorLabel.setFont(new Font("Times New Roman", 12));
            
            HBox uploadStatusPane = new HBox();
            uploadStatusPane.setSpacing(10);
            uploadLabel = new Label("Upload Status: ");
            uploadLabel.setFont(new Font("Times New Roman", 12));
            uploadStatus = new Label();
            uploadStatus.setFont(new Font("Times New Roman", 12));
            
            dbStatusPane.getChildren().addAll(dbStatusLabel, dbIndicatorLabel);
            lastUpdatedPane.getChildren().addAll(lastUpdatedLabel, lastUpdatedIndicatorLabel);
            uploadStatusPane.getChildren().addAll(uploadLabel, uploadStatus);
        leftCornerInfoPane.getChildren().addAll(dbStatusPane, lastUpdatedPane, uploadStatusPane);
        //leftCornerInfoPane End
        
        //rightCornerInfoPane Start
        VBox rightCornerInfoPane = new VBox();
        rightCornerInfoPane.setPadding(new Insets(10));
        rightCornerInfoPane.setSpacing(0);
        rightCornerInfoPane.setAlignment(Pos.CENTER_RIGHT);
        
            HBox autoArchiveStatusPane = new HBox();
            autoArchiveStatusPane.setSpacing(10);
            autoArchiveStatusPane.setAlignment(Pos.CENTER_RIGHT);
            autoArchiveStatusLabel = new Label("Folder Listener On:");
            autoArchiveStatusLabel.setFont(new Font("Times New Roman", 12));
            autoArchiveStatus = new Label(String.valueOf(autoArchiveOn));
            autoArchiveStatus.setFont(new Font("Times New Roman", 12));
            
            HBox autoArchiveProgressPane = new HBox();
            autoArchiveProgressPane.setSpacing(10);
            autoArchiveProgressPane.setAlignment(Pos.CENTER_RIGHT);
            autoArchiveProgressLabel = new Label("Auto Archive:");
            autoArchiveProgressLabel.setFont(new Font("Times New Roman", 12));
            autoArchiveProgress = new Label();
            autoArchiveProgress.setFont(new Font("Times New Roman", 12));
            
            HBox totalProcessedBox = new HBox();
            totalProcessedBox.setSpacing(10);
            totalProcessedBox.setAlignment(Pos.CENTER_RIGHT);
            totalProcessedLabel = new Label("Total Processed:");
            totalProcessedLabel.setFont(new Font("Times New Roman", 12));
            totalProcessedCount = new Label(String.valueOf(autoArchiveCount));
            totalProcessedCount.setFont(new Font("Times New Roman", 12));
            
            autoArchiveStatusPane.getChildren().addAll(autoArchiveStatusLabel, autoArchiveStatus);
            autoArchiveProgressPane.getChildren().addAll(autoArchiveProgressLabel, autoArchiveProgress);
            totalProcessedBox.getChildren().addAll(totalProcessedLabel,totalProcessedCount);
        rightCornerInfoPane.getChildren().addAll(autoArchiveStatusPane, autoArchiveProgressPane, totalProcessedBox);
        //rightCornerInfoPane end
        
        //Create 4 Buttons
        folderButton = new Button("Choose Folder");
        folderButton.setMinSize(3*TILE_SIZE, 1.5*TILE_SIZE);
        folderButton.setOnAction(this::folderChooser);
        folderButton.setDisable(true);
        startButton = new Button("Start Archive");
        startButton.setDisable(true);
        startButton.setMinSize(3*TILE_SIZE, TILE_SIZE);
        startButton.setOnAction(this::startArchive);
        pauseButton = new Button("Pause");
        pauseButton.setDisable(true);
        pauseButton.setMinSize(3*TILE_SIZE, TILE_SIZE);
        pauseButton.setOnAction(this::pauseArchive);
        cancelButton = new Button("Cancel");
        cancelButton.setDisable(true);
        cancelButton.setMinSize(3*TILE_SIZE, TILE_SIZE);
        cancelButton.setOnAction(this::cancelArchive);
        popUpWindowButton = new Button("View Archive Status");
        popUpWindowButton.setDisable(false);
        popUpWindowButton.setMinSize(6*TILE_SIZE, TILE_SIZE*.85);
        popUpWindowButton.setFont(new Font("Times new Roman", 18));
        popUpWindowButton.setOnAction(this::newWindow);
        popUpWindowButton2 = new Button("View Upload Status");
        popUpWindowButton2.setDisable(false);
        popUpWindowButton2.setMinSize(6*TILE_SIZE, TILE_SIZE*.85);
        popUpWindowButton2.setFont(new Font("Times new Roman", 18));
        popUpWindowButton2.setOnAction(this::uploadWindow);
        //Add Text Labels
        folderLabel = new Label("");
        folderLabel.setWrapText(true);
        folderLabel.setFont(ITALIC);
        folderLabel.setMinSize(13*TILE_SIZE, 2*TILE_SIZE);
        
        infoLabel = new Label("Welcome to GotRadio's MySQL Database Manager! Press"
                + " the 'Folder' Button in the Upper-Left Corner to Start.");
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);
        infoLabel.setFont(new Font("Times New Roman", 20));
        infoLabel.setMinSize(14*TILE_SIZE, 2*TILE_SIZE);
        
        statusLabel = new Label("");
        statusLabel.setFont(new Font("Times New Roman", 18));
        statusLabel.setMinSize(12*TILE_SIZE, TILE_SIZE);
        currentSessionLabel = new Label("Current Session:\t" + goodSongs 
                + " Successful Archives\n\t\t\t\t" + badSongs + " Unsuccessful Archives");
        currentSessionLabel.setFont(new Font("Times New Roman", 14));
        currentSessionLabel.setMinSize(8*TILE_SIZE, TILE_SIZE);
        totalSessionLabel = new Label("Total Session:\t" + totalGoodSongs
                + " Successful Archives\n\t\t\t" + totalBadSongs + " Unsuccessful Archives");
        totalSessionLabel.setFont(new Font("Times New Roman", 14));
        totalSessionLabel.setMinSize(8*TILE_SIZE, TILE_SIZE);
        
        
        
        //Add Progress Bar/Indicator
        pb = new ProgressBar();
        pb.setProgress(0.0);
        pb.setMinSize(12*TILE_SIZE, TILE_SIZE);
        pi = new ProgressIndicator();
        pi.setProgress(0.0);
        pi.setMinSize(3*TILE_SIZE, 3*TILE_SIZE);
        
        //Add Elements to GridPane
        gPane.add(menu, 0, 0, 24, 1);
        gPane.add(leftCornerInfoPane, 0, 1, 8, 1);
        gPane.add(folderButton, 1, 2, 3, 2);
        gPane.add(startButton, 1, 6, 3, 1);
        gPane.add(pauseButton, 1, 8, 3, 1);
        gPane.add(cancelButton, 1, 10, 3, 1);
        gPane.add(popUpWindowButton, 9, 10, 6, 1);
        gPane.add(popUpWindowButton2, 9, 11, 6, 1);
        gPane.add(picturePane, 19, 1, 4, 4);
        gPane.add(folderLabel, 5, 1, 13, 2);
        gPane.add(infoLabel, 5, 4, 13, 2);
        gPane.add(statusLabel, 6, 8, 12, 1);
        gPane.add(currentSessionLabel, 0, 11, 8, 1);
        gPane.add(totalSessionLabel, 16, 11, 8, 1);
        gPane.add(pb, 6, 9, 12, 1);
        gPane.add(pi, 18, 8, 3, 3);
        gPane.add(rightCornerInfoPane, 12, 1, 12, 1);
        
        scene = new Scene(gPane, 24*TILE_SIZE, 12*TILE_SIZE);
        
        stage.getIcons().add(applicationIcon);
        stage.setTitle("GotRadio, LLC. - MySQL Database Manager, " + versionNumber);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::exitProgram);    
        stage.show();
        
        databaseConnect();
        
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    private void folderChooser(ActionEvent event){
        try{
            checkDatabaseConnection();
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(folderSelected);
            folderSelected = directoryChooser.showDialog(stage);
            folderLabel.setText(folderSelected.getPath());
            startButton.setDisable(false);
            infoLabel.setText("Folder '" + folderSelected.getName() + "' has been selected."
                    + " When ready, please press the Start button to start the archive.");
        } catch (IllegalArgumentException e){
            folderSelected = new File(".");
        } catch (NullPointerException ex){
            folderSelected = new File("C:\\Users\\vstar\\Dropbox\\programmer_share\\upload_archive");
        }
    }
    
    private synchronized void startArchive(ActionEvent event){ 
        startArchiveMethod(this.folderSelected);
    }
    private synchronized void startArchiveMethod(File folderSelected){       
        checkDatabaseConnection();
        archiveRunning = true;
        
         Platform.runLater(new Runnable(){
            @Override
            public synchronized void run() {
                infoLabel.setText("Archiving songs from '" + folderSelected.getName() + "'"
                + ", please do not exit the application without pausing/canceling.");
                startButton.setText("Start Archive");
            }
        });
        
        folderButton.setDisable(true);
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        cancelButton.setDisable(true);
        
        //Create new Concurrent Task
        archiveWorker = new Task<Void>(){
            {
                updateMessage("Please wait, program is loading song list..");
            }
            
            @Override
            protected synchronized Void call() throws Exception {
                
                
                File[] fArr = folderSelected.listFiles();
                i = 1;
                goodSongs = 0;
                badSongs = 0;
                
                for(File f : fArr){
                    if(f.isFile()){
                        error = "";
                        updateMessage("Archiving File: " + f.getName());
                        MetaDataExtractor extract = new MetaDataExtractor(f);
                        SongValidation sv = new SongValidation(extract);
                        /* VERSION 2.0 NEED TO FIX
                        ObservableList<Song> row = FXCollections.observableArrayList();
                        */
                        boolean sqlArchived = false;
                        if(!sv.hasErrors()){
                            sqlArchived = uploadData(extract, f, folderSelected);
                        } else {
                            ArrayList<SongValidation.ErrorType> errors = sv.getErrorList();
                            try {
                                if(errors.contains(SongValidation.ErrorType.Missing_MP3_Extension)){
                                    //Move File to Missing MP3 Extension
                                    new File(folderSelected.getPath() + "\\Archive Fail - Missing MP3 Extension\\").mkdir();
                                    Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                            + "\\Archive Fail - Missing MP3 Extension\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                    error = "(Missing MP3 Extension)";
                                } else if(errors.contains(SongValidation.ErrorType.Incorrect_File_Type)){
                                    //Move File to Incorrect File Type
                                    new File(folderSelected.getPath() + "\\Archive Fail - Incorrect File Type\\").mkdir();
                                    Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                            + "\\Archive Fail - Incorrect File Type\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                    error = "(Incorrect File Type)";
                                } else if(errors.contains(SongValidation.ErrorType.Missing_Artist_Tag)
                                        || errors.contains(SongValidation.ErrorType.Missing_Title_Tag)
                                        || errors.contains(SongValidation.ErrorType.Missing_Genre_Tag)){
                                    //Move File to Metadata Missing_Unreadable
                                    new File(folderSelected.getPath() + "\\Archive Fail - Metadata Missing or Unreadable\\").mkdir();
                                    Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                            + "\\Archive Fail - Metadata Missing or Unreadable\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                    error = "(Metadata Missing or Unreadable)";
                                } else if(errors.contains(SongValidation.ErrorType.Filename_Tag_Mismatch)){
                                    //Move File to Filename Metadata Mismatch
                                    new File(folderSelected.getPath() + "\\Archive Fail - Filename Metadata Mismatch\\").mkdir();
                                    Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                            + "\\Archive Fail - Filename Metadata Mismatch\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                    error = "(Filename Metadata Mismatch)";
                                } else {
                                    //Move File to Incorrect Genre Tag
                                    new File(folderSelected.getPath() + "\\Archive Fail - Incorrect Genre Tag\\").mkdir();
                                    Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                            + "\\Archive Fail - Incorrect Genre Tag\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                    error = "(Incorrect Genre Tag)";
                                }
                            } catch (IOException e){
                                Alert alert = new Alert(AlertType.INFORMATION);
                                alert.setTitle("GRDatabaseManager: Unable to move Bad File");
                                alert.setContentText(e.getMessage());
                                alert.showAndWait();
                            }
                        }//End of SongValidation Check
                        
                        if(sqlArchived) {
                            //Move File to Archive Folder if not already in.
                            updateMessage("Successfully Archived: " + f.getName());
                            goodSongs++;
                            totalGoodSongs++;
                            goodList.add("...\\" + folderSelected.getName() + "\\" + f.getName());
                             //Move File to Incorrect Genre Tag
                             try {
                                    Files.move(Paths.get(f.getPath()), Paths.get(archiveFolder.getPath() 
                                            + "\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                             } catch (IOException e){
                                 Alert alert = new Alert(AlertType.INFORMATION);
                                alert.setTitle("GRDatabaseManager: Unable to move Good File");
                                alert.setContentText(e.getMessage());
                                alert.showAndWait();
                             }      
                             
                             //Send Song to Upload Task
                             pendingUploadList.add(f.getName());
                             System.out.println("Added song to pendingUploadList: " + f.getName());
                             
                        } else {
                            updateMessage("Failed to Archive: " + f.getName() + " " + error);
                            badSongs++;
                            totalBadSongs++;
                            badList.add(f.getName() + " " + error); 
                        }
                        
                        //Updates Application Running Song Count
                        Platform.runLater(new Runnable(){
                            @Override
                            public synchronized void run() {
                                currentSessionLabel.setText("Current Session:\t" + goodSongs 
                                    + " Successful Archives\n\t\t\t\t" + badSongs 
                                    + " Unsuccessful Archives");
                                totalSessionLabel.setText("Total Session:\t" + totalGoodSongs
                                    + " Successful Archives\n\t\t\t" + totalBadSongs 
                                    + " Unsuccessful Archives");
                                lastArchived = new java.sql.Timestamp(calendar.getTime().getTime());
                                lastUpdatedIndicatorLabel.setText(lastArchived.toString());
                                updateSongList(goodListView, goodList);
                                updateSongList(badListView, badList);
                            }
                        });
                        //End of Application GUI Update
                        
                        //Update Progress and Increment Counter
                        updateProgress(i, fArr.length);
                        i++;
                    }
                }
                
                updateProgress(100, 100);
                pauseButton.setDisable(true);
                cancelButton.setDisable(true);
                folderButton.setDisable(false);
                updateMessage("Archive 'COMPLETE'! The following has occurred:\n" 
                    + "Songs Successfully Archived: " + goodSongs + ", Songs Failed to Archive: " + badSongs);
                
                
                
                Platform.runLater(new Runnable(){
                            @Override
                            public synchronized void run() {
                                checkDatabaseConnection();
                                startUploadTasker();
                            }
                });
                
                archiveRunning = false;
                
                return null;
            }
        }; // End of concurrent Task
        
        pb.progressProperty().unbind();
        pb.progressProperty().bind(archiveWorker.progressProperty());
        pi.progressProperty().unbind();
        pi.progressProperty().bind(archiveWorker.progressProperty());
        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(archiveWorker.messageProperty());
        taskThread = new Thread(archiveWorker);
        taskThread.setDaemon(true);
        taskThread.start();

    }
    
    private void pauseArchive(ActionEvent event){
        if(pauseButton.getText().equals("Pause")){
            infoLabel.setText("Archive 'PAUSED'. Press 'Continue' to proceed "
                    + "with the current operation, or 'Cancel' to terminate the process.");
            taskThread.suspend();
            cancelButton.setDisable(false);
            pauseButton.setText("Continue");
            
        } else {
            infoLabel.setText("Archive 'RESUMED'. Archiving songs from '" 
                    + folderSelected.getName() + "', please do not exit the "
                            + "application without pausing/canceling.");
            taskThread.resume();
            cancelButton.setDisable(true);
            pauseButton.setText("Pause");
        }
    }
    
    private void cancelArchive(ActionEvent event){
        taskThread.interrupt();
        pb.progressProperty().unbind();
        pi.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        pb.setProgress(0);
        pi.setProgress(0);
        folderButton.setDisable(false);
        startButton.setDisable(true);
        pauseButton.setText("Pause");
        pauseButton.setDisable(true);
        cancelButton.setDisable(true);
        infoLabel.setText("Archive 'CANCELED'. When ready to proceed, please "
                + "choose a folder to archive songs into the MySQL Database.");
        statusLabel.setText("");
        archiveRunning = false;
    }
    
    private void updateSongList(ListView tableList, ArrayList<String> list){
       tableList.setItems(FXCollections.observableArrayList(list));
    } //End of updateSongList
    
    private void updateUploadList(ListView tableList, ArrayList<String> list){
       tableList.setItems(FXCollections.observableArrayList(list));
    } //End of updateSongList
    
    private void newWindow(ActionEvent event){
        Stage st = new Stage();
        VBox frameContainer = new VBox();
        frameContainer.setMinSize(24*TILE_SIZE, 12*TILE_SIZE);
        
        //Create Top Label Pane
        HBox topContainer = new HBox();
            HBox goodListLabelContainer = new HBox();
            HBox badListLabelContainer = new HBox();
            Label goodLabel = new Label("Files Successfully Archived to MySQL Database");
            goodLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 18));
            Label badLabel = new Label("Files Unsuccessful and Not Archived to MySQL Database");
            badLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 18));
            goodListLabelContainer.getChildren().add(goodLabel);
            badListLabelContainer.getChildren().add(badLabel);
            goodListLabelContainer.setMinSize(12*TILE_SIZE, TILE_SIZE);
            badListLabelContainer.setMinSize(12*TILE_SIZE, TILE_SIZE);
            goodListLabelContainer.setAlignment(Pos.CENTER);
            badListLabelContainer.setAlignment(Pos.CENTER);
        
        topContainer.getChildren().addAll(goodListLabelContainer, badListLabelContainer);
        //End of Top Container Creation
        
        //Create Container for Both Lists
        HBox listContainer = new HBox();
            listContainer.setMinSize(24*TILE_SIZE, 10*TILE_SIZE);

            updateSongList(goodListView, goodList);
            //updateTableList(goodTable, goodTableData); /////VERSION 2.0
            goodListView.setMinSize(12*TILE_SIZE, 10*TILE_SIZE);
            HBox goodContainer = new HBox();
            goodContainer.setMinSize(12*TILE_SIZE, 10*TILE_SIZE);
            goodContainer.getChildren().add(goodListView);

            updateSongList(badListView, badList);
            //updateTableList(badTable, badTableData); /////VERSION 2.0
            badListView.setMinSize(12*TILE_SIZE, 10*TILE_SIZE);
            HBox badContainer = new HBox();
            badContainer.setMinSize(12*TILE_SIZE, 10*TILE_SIZE);
            badContainer.getChildren().add(badListView);
        listContainer.getChildren().addAll(goodContainer, badContainer);
        //End of List Container
        
        //Create Bottom Container Total Labels
        HBox botContainer = new HBox();
            botContainer.setMinSize(24*TILE_SIZE, TILE_SIZE);
            HBox totalGoodCountContainer = new HBox();
            HBox totalBadCountContainer = new HBox();
            Label goodCountLabel = new Label("Total: " + totalGoodSongs);
            goodCountLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            goodCountLabel.setTextFill(Color.GREEN);
            totalGoodCountContainer.setAlignment(Pos.CENTER);
            Label badCountLabel = new Label("Total: " + totalBadSongs);
            badCountLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            badCountLabel.setTextFill(Color.RED);
            totalBadCountContainer.setAlignment(Pos.CENTER);
            totalGoodCountContainer.getChildren().add(goodCountLabel);
            totalBadCountContainer.getChildren().add(badCountLabel);
            totalGoodCountContainer.setMinSize(12*TILE_SIZE, TILE_SIZE);
            totalBadCountContainer.setMinSize(12*TILE_SIZE, TILE_SIZE);
        botContainer.getChildren().addAll(totalGoodCountContainer, totalBadCountContainer);
        //End of Bot Container
        
        frameContainer.getChildren().addAll(topContainer, listContainer, botContainer);
        Scene sc = new Scene(frameContainer);
        st.setTitle("GotRadio, LLC. - Processed Song Lists");
        st.setScene(sc);
        st.show();
    }
    
    private boolean uploadData(MetaDataExtractor extract, File f, File folderSelected){
        boolean uploaded = false;
        String artist = extract.getTikaArtist(), 
                title = extract.getTikaTitle(), 
                fileName = extract.getFileName(), 
                fileType = extract.getFileType(), 
                path = extract.getFilePath(),
                genreList = extract.getTikaGenre();
        Double size = extract.getFileSize();
        Time duration = extract.getSongDuration();
        
        calendar = Calendar.getInstance();
        
        java.sql.Date date_added = new java.sql.Date(calendar.getTime().getTime());
        java.sql.Time time_added = new java.sql.Time(calendar.getTime().getTime());
        PreparedStatement preparedStmt, preparedStmt2;
        try {
            String query = "replace into gotradio_db.songs (song_artist, song_title, "
                    + "date_added, time_added, genre_list, duration, size, "
                    + "file_name, file_type, path)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            preparedStmt = myConn.prepareStatement(query);
            preparedStmt.setString(1, artist);
            preparedStmt.setString(2, title);
            preparedStmt.setDate(3, date_added);
            preparedStmt.setTime(4, time_added);
            preparedStmt.setString(5, genreList);
            preparedStmt.setTime(6, duration);
            preparedStmt.setDouble(7, size);
            preparedStmt.setString(8, fileName);
            preparedStmt.setString(9, fileType);
            preparedStmt.setString(10, path);
            
            
            preparedStmt.execute();
            
            preparedStmt2 = serverConn.prepareStatement(query);
            preparedStmt2.setString(1, artist);
            preparedStmt2.setString(2, title);
            preparedStmt2.setDate(3, date_added);
            preparedStmt2.setTime(4, time_added);
            preparedStmt2.setString(5, genreList);
            preparedStmt2.setTime(6, duration);
            preparedStmt2.setDouble(7, size);
            preparedStmt2.setString(8, fileName);
            preparedStmt2.setString(9, fileType);
            preparedStmt2.setString(10, path);
            
            preparedStmt2.execute();
            
            uploaded = true;    
        } catch (SQLException e){
            //Move File to MySQL Fail Folder
            try {
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    new File(folderSelected.getPath() + "\\Archive Fail - Duplicate Song Found\\").mkdir();
                        Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                + "\\Archive Fail - Duplicate Song Found\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                    error = "(Duplicate Song Found)";
                } else {
                    new File(folderSelected.getPath() + "\\Archive Fail - MySQL Archive Fail\\").mkdir();
                        Files.move(Paths.get(f.getPath()), Paths.get(folderSelected.getPath() 
                                + "\\Archive Fail - MySQL Archive Fail\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                    error = "(MySQL Archive Fail)";
                }
            } catch (IOException ex){
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("GRDatabaseManager: Unable to move Bad File");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
            uploaded = false;
        }
        return uploaded;
    }
    

    
    private void uploadWindow(ActionEvent event){
        /*
            CopyStreamAdapter streamListener = new CopyStreamAdapter(){
                
                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize){
                    //
                }
            };
            ftp.setCopyStreamListener(streamListener);
        */
        Stage st = new Stage();
        VBox frameContainer = new VBox();
        frameContainer.setMinSize(24*TILE_SIZE, 12*TILE_SIZE);
        
        //Create Top Label Pane
        HBox topContainer = new HBox();
            HBox goodListLabelContainer = new HBox();
            HBox badListLabelContainer = new HBox();
            HBox pendingListLabelContainer = new HBox();
            Label goodListLabel = new Label("Files Successfully Uploaded");
            goodListLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            Label badListLabel = new Label("Files Failed to Upload");
            badListLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            Label pendingListLabel = new Label("Files Awaiting Upload");
            pendingListLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            goodListLabelContainer.getChildren().add(goodListLabel);
            badListLabelContainer.getChildren().add(badListLabel);
            pendingListLabelContainer.getChildren().add(pendingListLabel);
            goodListLabelContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            badListLabelContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            pendingListLabelContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            goodListLabelContainer.setAlignment(Pos.CENTER);
            badListLabelContainer.setAlignment(Pos.CENTER);
            pendingListLabelContainer.setAlignment(Pos.CENTER);
        
        topContainer.getChildren().addAll(goodListLabelContainer, badListLabelContainer, pendingListLabelContainer);
        //End of Top Container Creation
        
        //Create Container for Both Lists
        HBox listContainer = new HBox();
            listContainer.setMinSize(24*TILE_SIZE, 10*TILE_SIZE);

            updateUploadList(resultGoodUploadView, resultGoodUploadList);
            //updateTableList(goodTable, goodTableData); /////VERSION 2.0
            resultGoodUploadView.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            HBox resultGoodUploadContainer = new HBox();
            resultGoodUploadContainer.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            resultGoodUploadContainer.getChildren().add(resultGoodUploadView);

            updateUploadList(resultBadUploadView, resultBadUploadList);
            //updateTableList(goodTable, goodTableData); /////VERSION 2.0
            resultBadUploadView.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            HBox resultBadUploadContainer = new HBox();
            resultBadUploadContainer.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            resultBadUploadContainer.getChildren().add(resultBadUploadView);
            
            updateUploadList(pendingUploadView, pendingUploadList);
            //updateTableList(badTable, badTableData); /////VERSION 2.0
            pendingUploadView.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            HBox pendingUploadContainer = new HBox();
            pendingUploadContainer.setMinSize(8*TILE_SIZE, 10*TILE_SIZE);
            pendingUploadContainer.getChildren().add(pendingUploadView);
        listContainer.getChildren().addAll(resultGoodUploadContainer, resultBadUploadContainer, pendingUploadContainer);
        //End of List Container
        
        //Create Bottom Container Total Labels
        HBox botContainer = new HBox();
        botContainer.setMinSize(24*TILE_SIZE, TILE_SIZE);
            HBox totalGoodCountContainer = new HBox();
            totalGoodCountContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            totalGoodCountContainer.setAlignment(Pos.CENTER);
                totalGoodCountLabel = new Label("Total: " + resultGoodUploadList.size());
                totalGoodCountLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
                totalGoodCountLabel.setTextFill(Color.GREEN);
            totalGoodCountContainer.getChildren().add(totalGoodCountLabel);
            
            HBox totalBadCountContainer = new HBox();
            totalBadCountContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            totalBadCountContainer.setAlignment(Pos.CENTER);
                totalBadCountLabel = new Label("Total: " + resultBadUploadList.size());
                totalBadCountLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
                totalBadCountLabel.setTextFill(Color.RED);
            totalBadCountContainer.getChildren().add(totalBadCountLabel);
            
            HBox totalPendingCountContainer = new HBox();
            totalPendingCountContainer.setMinSize(8*TILE_SIZE, TILE_SIZE);
            totalPendingCountContainer.setAlignment(Pos.CENTER);
                pendingCountLabel = new Label("Total: " + pendingUploadList.size());
                pendingCountLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
                pendingCountLabel.setTextFill(Color.BLACK);
            totalPendingCountContainer.getChildren().add(pendingCountLabel);
            //End of Label Containers within Bot Container
            
        botContainer.getChildren().addAll(totalGoodCountContainer, totalBadCountContainer, totalPendingCountContainer);
        //End of Bot Container
        
        frameContainer.getChildren().addAll(topContainer, listContainer, botContainer);
        Scene sc = new Scene(frameContainer);
        st.setTitle("GotRadio, LLC. - Song Upload Status");
        st.setScene(sc);
        st.show();
    }
    
    private void startUploadTasker(){
        
        folderButton.setDisable(true);
        pauseButton.setDisable(true);
        cancelButton.setDisable(true);
        Platform.runLater(new Runnable(){
            @Override
            public synchronized void run() {
                uploadStatus.setText("In Progress..");
                uploadStatus.setTextFill(Color.RED);
                infoLabel.setText("WARNING: Do not close application, songs are currently being uploaded to server!");
                infoLabel.setTextFill(Color.RED);
            }
        });
        
        uploadTasker = new Task<Void>(){
            
            @Override
            protected synchronized Void call() throws Exception {
                String host = "";
                String user = "";
                String pass = "";
                client = new FTP_Client();
                int listSize = pendingUploadList.size();
                        for(int i = 0; i < listSize; i++){
                            client.connect(host, user, pass);
                            String currentUploadString = pendingUploadList.get(0);
                            File currentUploadFile = new File(archiveFolder + "\\" + currentUploadString);

                            if(client.uploadFile(currentUploadFile)){
                                resultGoodUploadList.add(currentUploadString);
                                System.out.println("Successfully uploaded: " + currentUploadFile.getName());
                            } else {
                                
                                System.out.println("FTP Client Upload Failed, Attempting SFTP Upload.");
                                
                                try {
                                    Channel channel = databaseConnect.getSession().openChannel("sftp");
                                    channel.connect();
                                    ChannelSftp sftp = (ChannelSftp) channel;
                                    sftp.cd("c:\\media\\new\\");
                                    sftp.put(new FileInputStream(currentUploadFile), currentUploadFile.getName(), ChannelSftp.OVERWRITE);
                                    sftp.exit();
                                } catch (Exception sftpException){
                                    System.out.println("SFTP: Failed to Upload File, adding to bad upload list.");
                                    
                                    resultBadUploadList.add(currentUploadString);
                                    System.out.println("Failed to upload: " + currentUploadFile.getName());
                                    try {
                                        System.out.println("GRDatabaseManager: Copying Song to Upload Fail..");
                                        new File("D:\\Music Library Clean\\Upload Fail - Unable to Upload").mkdir();
                                        Files.copy(Paths.get(currentUploadFile.getPath()), Paths.get("D:\\Music Library Clean\\Upload Fail - Unable to Upload\\" + currentUploadFile.getName()), StandardCopyOption.REPLACE_EXISTING);
                                        System.out.println("GRDatabaseManager: Song Successfully Copied, resuming operation.");
                                    } catch (IOException e){
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setTitle("GRDatabaseManager: Unable to move Bad File");
                                        alert.setContentText("Failed to Copy: " + currentUploadFile.getName() +
                                                "\nDestination: Music Library Clean\\Upload Fail = Unable to Upload");
                                        alert.showAndWait();
                                    }
                                }              
                            }
                            
                            //Copying Song to Media Folder
                            String copyStatus = "Failed";
                            String command = "copy \"c:\\media\\new\\" + currentUploadString + "\" c:\\media\\";
                            Channel channel = databaseConnect.getSession().openChannel("exec");
                            ((ChannelExec)channel).setCommand(command);
                            channel.setInputStream(null);
                            ((ChannelExec)channel).setErrStream(System.err);
                            InputStream in=channel.getInputStream();

                            try {
                            channel.connect();
                            byte[] tmp=new byte[1024];
                            while(true){
                              while(in.available()>0){
                                int j=in.read(tmp, 0, 1024);
                                if(j<0)break;
                              }    
                                if(channel.isClosed()){
                                    if(in.available()>0) continue;
                                    if(channel.getExitStatus()==0){
                                        copyStatus = "Succeeded";
                                        System.out.println("Successfully Copied Song: " + currentUploadString);
                                    } else {
                                        System.out.println("Failed to Copy Song: " + currentUploadString);
                                    }
                                    break;
                                }
                            }   
                            channel.disconnect();
                            } catch (Exception e){}
                            
                            
                            
                            
                            pendingUploadList.remove(currentUploadString);

                            Platform.runLater(new Runnable(){
                                @Override
                                public synchronized void run() {
                                    updateUploadList(resultGoodUploadView, resultGoodUploadList);
                                    updateUploadList(resultBadUploadView, resultBadUploadList);
                                    updateUploadList(pendingUploadView, pendingUploadList);
                                    totalGoodCountLabel.setText("Total: " + resultGoodUploadList.size());
                                    totalBadCountLabel.setText("Total: " + resultBadUploadList.size());
                                    pendingCountLabel.setText("Total: " + pendingUploadList.size());
                                }
                            });
                            client.disconnect();
                        } //End of PendingList Loop
        
                folderButton.setDisable(false);
                Platform.runLater(new Runnable(){
                    @Override
                    public synchronized void run() {
                        uploadStatus.setText("Completed.");
                        uploadStatus.setTextFill(Color.GREEN);
                        infoLabel.setText("Upload Status Complete! You may now resume or exit application.");
                        infoLabel.setTextFill(Color.BLACK);
                    }
                });
                return null;
            }
        };
        uploadThread = new Thread(uploadTasker);
        uploadThread.setDaemon(true);
        uploadThread.start();
    }
    
    private void checkDatabaseConnection(){
        try{
            if(!myConn.isValid(10) || !client.getFTP().isConnected() || !databaseConnect.isConnected()){
                databaseConnect();
            }
        } catch (SQLException e){
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("MySQL Localhost Connection Failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void databaseConnect(){
        
        
        //Testing DB Connection
        try {
            myConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "username", "pw");
            dbConnection = true;
        } catch (SQLException e) {
            dbConnection = false;  
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("MySQL Localhost onnection Failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            //e.printStackTrace();
        }
        
        //Testing Server Connection
        databaseConnect = new DatabaseConnect( 
                "scellis13", 
                "Puminist3r13!", 
                dbIndicatorLabel);
        serverDatabaseConnection = databaseConnect.databaseConnect();
        serverConn = databaseConnect.getSQLConnection();
        String host = "144.217.77.94";
        String user = "vstarr";
        String pass = "pr65WKSavETS";
        client = new FTP_Client();

        transferConnection = client.connect(host, user, pass);

        if(!dbConnection){
            infoLabel.setText("Error! Failed to connect to the Database. Please"
                    + " contact the Database Administrator for the proper Login"
                    + " Information.");
        }
        
        if(!transferConnection){
            infoLabel.setText("Error! Failed to connect to the Server. Please"
                    + " contact the Database Administrator for the proper Login"
                    + " Information.");
        }
        
        if(!databaseConnect.isConnected()){
            infoLabel.setText("Error! Failed to connect to the Server Database. Please"
                    + " contact the Database Administrator for the proper Login"
                    + " Information.");
        }
        
        Platform.runLater(()->{
            if(dbConnection && transferConnection && serverDatabaseConnection){
                folderButton.setDisable(false);
                dbIndicatorLabel.setText("Connected.");
                dbIndicatorLabel.setTextFill(Color.GREEN);
            } else {
                dbIndicatorLabel.setText("Failed.");
                dbIndicatorLabel.setTextFill(Color.RED);
            }
        });
        
    }
    
    private void autoArchiveSettingsWindow(ActionEvent event){
        if(!autoArchiveSettingsStage.isShowing()){
            System.out.println("Opening Auto Archive Settings.");

            autoArchiveSettingsStage = new Stage();

                VBox settingsVBox = new VBox();
                settingsVBox.setSpacing(20);
                settingsVBox.setPadding(new Insets(10, 10, 10, 10));
                settingsVBox.setAlignment(Pos.TOP_LEFT);
                settingsVBox.setMinSize(12*TILE_SIZE, 4*TILE_SIZE);
                settingsVBox.setMaxHeight(4*TILE_SIZE);
                settingsVBox.setStyle(AUTO_ARCHIVE_SETTINGS_STYLE);

                    HBox autoCheckBoxContainer = new HBox();
                    autoCheckBoxContainer.setAlignment(Pos.CENTER_LEFT);
                    autoCheckBoxContainer.setSpacing(10);
                        autoArchiveCheckBox = new CheckBox();
                            if(autoArchiveOn){
                                autoArchiveCheckBox.setSelected(true);
                            } else {
                                autoArchiveCheckBox.setSelected(false);
                            }
                            autoArchiveCheckBox.setOnAction(e -> {
                                if(!autoArchiveRunning){
                                    autoArchiveSaveButton.setDisable(false);
                                    autoArchiveSaveButtonLabel.setText("");
                                }
                            });
                        Label autoCheckBoxLabel = new Label("Turn Auto Archive On/Off");
                    autoCheckBoxContainer.getChildren().addAll(autoArchiveCheckBox, autoCheckBoxLabel);

                    HBox autoFolderHBox = new HBox();
                    autoFolderHBox.setAlignment(Pos.CENTER_RIGHT);
                    autoFolderHBox.setSpacing(10);
                        Button autoFolderChooser = new Button("Directory to Listen");
                        autoFolderChooser.setOnAction(this::autoArchiveFolderChooser);
                        autoArchiveFolderTempLabel = new Label(autoArchiveFolderSelected.getPath());
                    autoFolderHBox.getChildren().addAll(autoFolderChooser, autoArchiveFolderTempLabel);

                    HBox autoSaveHBox = new HBox();
                        autoArchiveSaveButtonLabel = new Label("");

                        autoArchiveSaveButton = new Button("Save");
                        autoArchiveSaveButton.setDisable(true);
                        autoArchiveSaveButton.setOnAction(this::saveAutoArchiveSettings);
                    autoSaveHBox.setSpacing(10);
                    autoSaveHBox.setAlignment(Pos.CENTER_RIGHT);
                    autoSaveHBox.getChildren().addAll(autoArchiveSaveButtonLabel, autoArchiveSaveButton);
                settingsVBox.getChildren().addAll(autoCheckBoxContainer, autoFolderHBox, autoSaveHBox);
            
            if(autoArchiveRunning){
                autoArchiveSaveButton.setDisable(true);
                autoArchiveSaveButtonLabel.setText("Auto Archive currently in session, please wait to change settings.");
                autoArchiveSaveButtonLabel.setStyle("-fx-text-fill: red;");
            }    
                
            Scene settingsScene = new Scene(settingsVBox);
            autoArchiveSettingsStage.setAlwaysOnTop(true);
            autoArchiveSettingsStage.setTitle("GotRadio, LLC. - Song Upload Status");
            autoArchiveSettingsStage.setScene(settingsScene);
            autoArchiveSettingsStage.show();
        }
    }
    
    private void autoArchiveFolderChooser(ActionEvent event){
        if(!autoArchiveRunning){
            try{
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(autoArchiveFolderSelected);

                autoArchiveFolderSelectedTemp = directoryChooser.showDialog(stage);
                autoArchiveFolderTempLabel.setText(autoArchiveFolderSelectedTemp.getPath());
                
                
                autoArchiveSaveButton.setDisable(false);
            } catch (IllegalArgumentException e){
                autoArchiveFolderSelected = new File(".");
            } catch (NullPointerException ex){
                autoArchiveFolderSelected = new File("C:\\Users\\vstar\\Dropbox\\programmer_share\\upload_archive");
            }
        } else {
            autoArchiveSaveButtonLabel.setText("Auto Archive currently in session, please wait to change settings.");
            autoArchiveSaveButtonLabel.setStyle("-fx-text-fill: red;");
        }
    }
    
    private void saveAutoArchiveSettings(ActionEvent event){
        if(autoArchiveSettingsStage.isShowing()){
            autoArchiveSettingsStage.close();
        }
        checkDatabaseConnection();
        autoArchiveFolderSelected = autoArchiveFolderSelectedTemp;
        

        listenerFolder.setText(autoArchiveFolderSelected.getPath());

        //Save Settings
        if(autoArchiveCheckBox.isSelected()){
            autoArchiveOn = true;
            autoArchiveProgress.setText("Listening...");
            listenerStatus.setText("On");
            currentProcess = "Listening..";
            listenerCurrentProcess.setText(currentProcess);
            //Start Listener Task
            autoArchiveTask = new Task<Void>(){

            @Override
            protected synchronized Void call() throws Exception {
                listener = new FileListener(autoArchiveFolderSelected, fileList);
                
                fileList.addListener(new ListChangeListener<String>() {
                    @Override
                    public void onChanged(javafx.collections.ListChangeListener.Change<? extends String> c){
                        try{
                            if(c.next()){
                                if(c.wasAdded()){
                                    if(autoArchiveCheckBox.isSelected()){
                                        checkDatabaseConnection();
                                        
                                        try{
                                            System.out.println("Pausing Thread.");
                                            Thread.sleep(5000);
                                        } catch (InterruptedException e){};
                                        
                                        checkDatabaseConnection(); //First Check sometimes fails, this second check ensures a reconnection
                                        //is established
                                        
                                        while(!fileList.isEmpty()){
                                            for(String fileString : fileList){
                                                File f = new File(autoArchiveFolderSelected.getPath() + "\\" + fileString);
                                                if(f.exists() && f.isFile()){
                                                    autoArchiveRunning = true;
                                                    autoArchiveCount++;
                                                    AutoArchiveFile tableViewFile = new AutoArchiveFile(new java.sql.Timestamp(calendar.getTime().getTime()), f.getName());
                                                    autoArchiveFileList.add(0, tableViewFile);
                                                    Platform.runLater(new Runnable(){
                                                        @Override
                                                        public synchronized void run() {
                                                            autoArchiveProgress.setText("Processing: " + f.getName());
                                                            autoArchiveProgress.setStyle("-fx-text-fill: red;");
                                                            tableViewFile.setArchiveStatus(AutoArchiveFile.ProgressEnum.IN_PROGRESS);
                                                            autoArchiveTableView.refresh();
                                                            totalAutoArchiveCount.setText(String.valueOf(autoArchiveCount));
                                                            totalProcessedCount.setText(String.valueOf(autoArchiveCount));
                                                            currentProcess = "Archiving..";
                                                            listenerCurrentProcess.setText(currentProcess);
                                                            currentAutoFile = f.getName();
                                                            listenerCurrentFile.setText(currentAutoFile);
                                                        }
                                                    });
                                                    
                                                    autoArchiveError = "";
                                                    MetaDataExtractor extract = new MetaDataExtractor(f);
                                                    SongValidation sv = new SongValidation(extract);

                                                    boolean sqlArchived = false;
                                                    if(!sv.hasErrors()){
                                                        sqlArchived = uploadData(extract, f, autoArchiveFolderSelected);
                                                        autoArchiveSuccessCount++;
                                                    } else {
                                                        ArrayList<SongValidation.ErrorType> errors = sv.getErrorList();
                                                        try {
                                                            if(errors.contains(SongValidation.ErrorType.Missing_MP3_Extension)){
                                                                //Move File to Missing MP3 Extension
                                                                new File(autoArchiveFolderSelected.getPath() + "\\Archive Fail - Missing MP3 Extension\\").mkdir();
                                                                Files.move(Paths.get(f.getPath()), Paths.get(autoArchiveFolderSelected.getPath() 
                                                                        + "\\Archive Fail - Missing MP3 Extension\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                autoArchiveError = "(Missing MP3 Extension)";
                                                            } else if(errors.contains(SongValidation.ErrorType.Incorrect_File_Type)){
                                                                //Move File to Incorrect File Type
                                                                new File(autoArchiveFolderSelected.getPath() + "\\Archive Fail - Incorrect File Type\\").mkdir();
                                                                Files.move(Paths.get(f.getPath()), Paths.get(autoArchiveFolderSelected.getPath() 
                                                                        + "\\Archive Fail - Incorrect File Type\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                autoArchiveError = "(Incorrect File Type)";
                                                            } else if(errors.contains(SongValidation.ErrorType.Missing_Artist_Tag)
                                                                    || errors.contains(SongValidation.ErrorType.Missing_Title_Tag)
                                                                    || errors.contains(SongValidation.ErrorType.Missing_Genre_Tag)){
                                                                //Move File to Metadata Missing_Unreadable
                                                                new File(autoArchiveFolderSelected.getPath() + "\\Archive Fail - Metadata Missing or Unreadable\\").mkdir();
                                                                Files.move(Paths.get(f.getPath()), Paths.get(autoArchiveFolderSelected.getPath() 
                                                                        + "\\Archive Fail - Metadata Missing or Unreadable\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                autoArchiveError = "(Metadata Missing or Unreadable)";
                                                            } else if(errors.contains(SongValidation.ErrorType.Filename_Tag_Mismatch)){
                                                                //Move File to Filename Metadata Mismatch
                                                                new File(autoArchiveFolderSelected.getPath() + "\\Archive Fail - Filename Metadata Mismatch\\").mkdir();
                                                                Files.move(Paths.get(f.getPath()), Paths.get(autoArchiveFolderSelected.getPath() 
                                                                        + "\\Archive Fail - Filename Metadata Mismatch\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                autoArchiveError = "(Filename Metadata Mismatch)";
                                                            } else {
                                                                //Move File to Incorrect Genre Tag
                                                                new File(autoArchiveFolderSelected.getPath() + "\\Archive Fail - Incorrect Genre Tag\\").mkdir();
                                                                Files.move(Paths.get(f.getPath()), Paths.get(autoArchiveFolderSelected.getPath() 
                                                                        + "\\Archive Fail - Incorrect Genre Tag\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                autoArchiveError = "(Incorrect Genre Tag)";
                                                            }
                                                        } catch (IOException e){
                                                            Alert alert = new Alert(AlertType.INFORMATION);
                                                            alert.setTitle("GRDatabaseManager Auto Archive: Unable to move Bad File");
                                                            alert.setContentText(e.getMessage());
                                                            alert.showAndWait();
                                                        }
                                                    }//End of SongValidation Check
                                                    
                                                    if(sqlArchived) {
                                                        System.out.println("File Successfully Archived in DB.");
                                                        //Upload Auto Archived Song
                                                        Platform.runLater(new Runnable(){
                                                            @Override
                                                            public synchronized void run() {
                                                                autoArchiveProgress.setText("Uploading " + f.getName());
                                                                currentProcess = "Uploading..";
                                                                listenerCurrentProcess.setText(currentProcess);
                                                                totalAutoArchiveResultSuccessCount.setText(String.valueOf(autoArchiveSuccessCount));
                                                                tableViewFile.setTime(new java.sql.Timestamp(calendar.getTime().getTime()));
                                                                tableViewFile.setArchiveStatus(AutoArchiveFile.ProgressEnum.SUCCEEDED);
                                                                tableViewFile.setUploadStatus(AutoArchiveFile.ProgressEnum.IN_PROGRESS);
                                                                autoArchiveTableView.refresh();
                                                            }
                                                        });

                                                        String host = "host";
                                                        String user = "user";
                                                        String pass = "pw";
                                                        client = new FTP_Client();

                                                        client.connect(host, user, pass);

                                                        boolean uploaded = client.uploadFile(f);
                                                        
                                                        if(uploaded){
                                                            System.out.println("Successfully uploaded: " + f.getName());
                                                            autoArchiveUploadSuccessCount++;
                                                            
                                                            Platform.runLater(new Runnable(){
                                                                @Override
                                                                public synchronized void run() {

                                                                    autoArchiveProgress.setText("Uploaded " + f.getName());
                                                                    autoArchiveProgress.setStyle("-fx-text-fill: green;");
                                                                    tableViewFile.setTime(new java.sql.Timestamp(calendar.getTime().getTime()));
                                                                    tableViewFile.setUploadStatus(AutoArchiveFile.ProgressEnum.SUCCEEDED);
                                                                    autoArchiveTableView.refresh();
                                                                }
                                                            });
                                                            
                                                        } else {
                                                            
                                                            System.out.println("FTP Client Upload Failed, Attempting SFTP Upload.");
                                
                                                            try {
                                                                Channel channel = databaseConnect.getSession().openChannel("sftp");
                                                                channel.connect();
                                                                ChannelSftp sftp = (ChannelSftp) channel;
                                                                sftp.cd("c:\\media\\new\\");
                                                                sftp.put(new FileInputStream(f), f.getName(), ChannelSftp.OVERWRITE);
                                                                uploaded = true;
                                                                sftp.exit();
                                                            } catch (Exception sftpException){
                                                                System.out.println(sftpException.getMessage());
                                                                System.out.println("SFTP: Failed to Upload File, adding to bad upload list.");
                                                                System.out.println("Failed to upload: " + f.getName());
                                                                autoArchiveUploadFailCount++;

                                                                Platform.runLater(new Runnable(){
                                                                    @Override
                                                                    public synchronized void run() {

                                                                        autoArchiveProgress.setText("Failed to Upload " + f.getName());
                                                                        autoArchiveProgress.setStyle("-fx-text-fill: red;");
                                                                        tableViewFile.setTime(new java.sql.Timestamp(calendar.getTime().getTime()));
                                                                        tableViewFile.setUploadStatus(AutoArchiveFile.ProgressEnum.FAILED);
                                                                        autoArchiveTableView.refresh();
                                                                    }
                                                                });

                                                                try {
                                                                    System.out.println("GRDatabaseManager: Copying Song to Upload Fail..");
                                                                    new File("D:\\Music Library Clean\\Upload Fail - Unable to Upload").mkdir();
                                                                    Files.copy(Paths.get(f.getPath()), Paths.get("D:\\Music Library Clean\\Upload Fail - Unable to Upload\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                                } catch (IOException e){
                                                                    Alert alert = new Alert(AlertType.INFORMATION);
                                                                    alert.setTitle("GRDatabaseManager: Unable to move Bad File");
                                                                    alert.setContentText("Failed to Copy: " + f.getName() +
                                                                            "\nDestination: Music Library Clean\\Upload Fail = Unable to Upload");
                                                                    alert.showAndWait();
                                                                }
                                                            }
                                                        }
                                                        client.disconnect();
                                                         //END OF SONG UPLOAD
                                                        
                                                        if(uploaded){
                                                            //Move File to Server Media Folder
                                                            try {
                                                                //Copying Song to Media Folder
                                                                String copyStatus = "Failed";
                                                                String command = "copy \"c:\\media\\new\\" + f.getName() + "\" c:\\media\\";
                                                                Channel channel = databaseConnect.getSession().openChannel("exec");
                                                                ((ChannelExec)channel).setCommand(command);
                                                                channel.setInputStream(null);
                                                                ((ChannelExec)channel).setErrStream(System.err);
                                                                InputStream in=channel.getInputStream();

                                                                try {
                                                                channel.connect();
                                                                byte[] tmp=new byte[1024];
                                                                while(true){
                                                                  while(in.available()>0){
                                                                    int j=in.read(tmp, 0, 1024);
                                                                    if(j<0)break;
                                                                  }    
                                                                    if(channel.isClosed()){
                                                                        if(in.available()>0) continue;
                                                                        if(channel.getExitStatus()==0){
                                                                            copyStatus = "Succeeded";
                                                                            System.out.println("Successfully Copied Song: " + f.getName());
                                                                        } else {
                                                                            System.out.println("Failed to Copy Song: " + f.getName());
                                                                        }
                                                                        break;
                                                                    }
                                                                }   
                                                                channel.disconnect();
                                                                } catch (Exception e){}
                                                            } catch (Exception copyException){}
                                                        }//End of copying file to new
                                                        
                                                         //Move File Archive Folder
                                                         try {
                                                                Files.move(Paths.get(f.getPath()), Paths.get(archiveFolder.getPath() 
                                                                        + "\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
                                                         } catch (IOException e){
                                                             Alert alert = new Alert(AlertType.INFORMATION);
                                                            alert.setTitle("GRDatabaseManager: Unable to move Good File");
                                                            alert.setContentText(e.getMessage());
                                                            alert.showAndWait();
                                                         }      

                                                    } else { //If-Else sqlArchived boolean
                                                        System.out.print("Failed to Auto Archive: " + f.getName() + " " + autoArchiveError);
                                                        autoArchiveFailCount++;
                                                        Platform.runLater(new Runnable(){
                                                            @Override
                                                            public synchronized void run() {
                                                                tableViewFile.setTime(new java.sql.Timestamp(calendar.getTime().getTime()));
                                                                tableViewFile.setArchiveStatus(AutoArchiveFile.ProgressEnum.FAILED);
                                                                tableViewFile.setUploadStatus(AutoArchiveFile.ProgressEnum.FAILED);
                                                                tableViewFile.setErrorMessage(autoArchiveError);
                                                                autoArchiveTableView.refresh();
                                                            }
                                                        });
                                                    }
                                                    
                                                    //Update count variables
                                                    Platform.runLater(new Runnable(){
                                                            @Override
                                                            public synchronized void run() {
                                                                totalAutoArchiveResultSuccessCount.setText(String.valueOf(autoArchiveSuccessCount));
                                                                totalAutoArchiveResultFailCount.setText(String.valueOf(autoArchiveFailCount));
                                                                totalAutoArchiveUploadResultSuccessCount.setText(String.valueOf(autoArchiveUploadSuccessCount));
                                                                totalAutoArchiveUploadResultFailCount.setText(String.valueOf(autoArchiveUploadFailCount));
                                                            }
                                                        });
                                                    
                                                }
                                                fileList.remove(fileString);

                                                Platform.runLater(new Runnable(){
                                                    @Override
                                                    public synchronized void run() {

                                                        currentProcess = "Listening..";
                                                        listenerCurrentProcess.setText(currentProcess);
                                                        currentAutoFile = "";
                                                        listenerCurrentFile.setText(currentAutoFile);
                                                        autoArchiveProgress.setText("Listening...");
                                                        autoArchiveProgress.setStyle("-fx-text-fill: black;");;
                                                    }
                                                });
                                                autoArchiveRunning = false;
                                            }
                                        }
                                    }//If-Statement Checking if checkbox still selected
                                }
                            }
                        } catch (ConcurrentModificationException e){};
                    }
                });
                listener.startService();
        
                return null;
            }
            
            };
            
            autoArchiveThread = new Thread(autoArchiveTask);
            autoArchiveThread.setDaemon(true);
            autoArchiveThread.start();
        } else {
            while(autoArchiveRunning){
                //Wait for current auto archive to finish
                System.out.println("Waiting for current file to finish archiving...");
            }
            listenerStatus.setText("Off");
            listenerCurrentProcess.setText("Not Active.");
            listener.stopService();
            autoArchiveThread.interrupt();
            autoArchiveOn = false;
            autoArchiveProgress.setText("Not Active.");
        }
        
        autoArchiveStatus.setText(String.valueOf(autoArchiveOn));
        autoArchiveSaveButtonLabel.setStyle("-fx-text-fill: green;");
        autoArchiveSaveButtonLabel.setText("Settings Saved.");
        autoArchiveSaveButton.setDisable(true);
        
    }
    
    private void autoArchiveDisplayWindow(ActionEvent event){
        System.out.println("Opening Auto Archive Display Window.");
        
        Stage autoArchiveStage = new Stage();
            BorderPane autoArchiveDisplayPane = new BorderPane();
            autoArchiveDisplayPane.setMinSize(16*TILE_SIZE,16*TILE_SIZE);
            autoArchiveDisplayPane.setPadding(new Insets(10,10,10,10));
            autoArchiveDisplayPane.setStyle("-fx-font-family: Times New Roman; -fx-font-size: 14;");
                //Top Pane
                VBox autoArchiveDisplayTopPane = new VBox();
                autoArchiveDisplayTopPane.setMinHeight(3*TILE_SIZE);
                autoArchiveDisplayTopPane.setSpacing(5);
                
                    HBox listenerStatusBox = new HBox();
                    listenerStatusBox.setSpacing(5);
                        Label listenerStatusLabel = new Label("Listener Status:");
                        listenerStatusLabel.setStyle("-fx-font-weight: bold;");
                        listenerStatus = new Label("");
                        if(autoArchiveOn){
                            listenerStatus.setText("On");
                        } else {
                            listenerStatus.setText("Off");
                        }
                    listenerStatusBox.getChildren().addAll(listenerStatusLabel,listenerStatus);
                    
                    HBox listenerFolderBox = new HBox();
                    listenerFolderBox.setSpacing(5);
                        Label listenerFolderLabel = new Label("Folder Listening:");
                        listenerFolderLabel.setStyle("-fx-font-weight: bold;");
                        listenerFolder = new Label(autoArchiveFolderSelected.getPath());
                    listenerFolderBox.getChildren().addAll(listenerFolderLabel,listenerFolder);
                    
                    HBox listenerCurrentProcessBox = new HBox();
                    listenerCurrentProcessBox.setSpacing(5);
                        Label listenerCurrentProcessLabel = new Label("Current Process:");
                        listenerCurrentProcessLabel.setStyle("-fx-font-weight: bold;");
                        listenerCurrentProcess = new Label(currentProcess);
                        
                    listenerCurrentProcessBox.getChildren().addAll(listenerCurrentProcessLabel,listenerCurrentProcess);
                    
                    HBox listenerCurrentFileBox = new HBox();
                    listenerCurrentFileBox.setSpacing(5);
                        Label listenerCurrentFileLabel = new Label("Current File:");
                        listenerCurrentFileLabel.setStyle("-fx-font-weight: bold;");
                        listenerCurrentFile = new Label(currentAutoFile);
                    listenerCurrentFileBox.getChildren().addAll(listenerCurrentFileLabel,listenerCurrentFile);
                    
                autoArchiveDisplayTopPane.getChildren().addAll(listenerStatusBox,listenerFolderBox,listenerCurrentProcessBox,listenerCurrentFileBox);
                //End Top Pane
            autoArchiveDisplayPane.setTop(autoArchiveDisplayTopPane);
                
                //Middle Pane
                VBox autoArchiveDisplayMidPane = new VBox();
                autoArchiveDisplayMidPane.setMinHeight(11*TILE_SIZE);
                autoArchiveDisplayMidPane.setStyle("-fx-background-color: gainsboro; -fx-border-color: black;");
                
                    HBox totalAutoArchiveCountBox = new HBox();
                    totalAutoArchiveCountBox.setSpacing(5);
                    totalAutoArchiveCountBox.setMinHeight(.75*TILE_SIZE);
                    totalAutoArchiveCountBox.setAlignment(Pos.CENTER_LEFT);
                    totalAutoArchiveCountBox.setPadding(new Insets(0, 0, 0, 10));
                        Label totalAutoArchiveCountLabel = new Label("Total Processed:");
                        totalAutoArchiveCount = new Label();
                        totalAutoArchiveCount.setText(String.valueOf(autoArchiveCount));
                    totalAutoArchiveCountBox.getChildren().addAll(totalAutoArchiveCountLabel, totalAutoArchiveCount);
                
                    //Table View Info
                    autoArchiveTableView = new TableView();
                    autoArchiveTableView.setMinHeight(10.25*TILE_SIZE);
                    autoArchiveTableView.setPrefHeight(10.25*TILE_SIZE);
                    autoArchiveTableView.setStyle("-fx-font-family: Times New Roman; -fx-font-size: 12; -fx-alignment: CENTER;");
                        //Table Columns
                        dateTimeCol = new TableColumn("Last Process Timestamp");
                            dateTimeCol.setCellValueFactory(new PropertyValueFactory("timeFileProcessed"));
                            dateTimeCol.setMinWidth(4*TILE_SIZE);
                        TableColumn<AutoArchiveFile, String> fileNameCol = new TableColumn("File Name");
                            fileNameCol.setCellValueFactory(new PropertyValueFactory("fileName"));
                            fileNameCol.setMinWidth(4*TILE_SIZE);
                        TableColumn<AutoArchiveFile, AutoArchiveFile.ProgressEnum> archiveStatusCol = new TableColumn("Archive Status");
                            archiveStatusCol.setCellValueFactory(new PropertyValueFactory("archiveStatus"));
                            archiveStatusCol.setMinWidth(4*TILE_SIZE);
                        TableColumn<AutoArchiveFile, AutoArchiveFile.ProgressEnum> uploadStatusCol = new TableColumn("Upload Status");
                            uploadStatusCol.setCellValueFactory(new PropertyValueFactory("uploadStatus"));
                            uploadStatusCol.setMinWidth(4*TILE_SIZE);
                        TableColumn<AutoArchiveFile, String> errorMessageCol = new TableColumn("Error (If Applicable)");
                            errorMessageCol.setCellValueFactory(new PropertyValueFactory("errorMessage"));
                            errorMessageCol.setMinWidth(6*TILE_SIZE);
                    autoArchiveTableView.getColumns().addAll(dateTimeCol, fileNameCol, archiveStatusCol, uploadStatusCol, errorMessageCol);
                    
                    //Iteration through Columns for Styling
                    ObservableList<TableColumn> columns = autoArchiveTableView.getColumns();
                    for(TableColumn col : columns){
                        col.setStyle("-fx-alignment: CENTER;");
                    }

                    autoArchiveTableView.setItems(autoArchiveFileList);
                    //End of Table View
                    
                autoArchiveDisplayMidPane.getChildren().addAll(totalAutoArchiveCountBox, autoArchiveTableView);
                //End Middle Pane
            autoArchiveDisplayPane.setCenter(autoArchiveDisplayMidPane);
            
                //Bottom Pane
                HBox autoArchiveDisplayBotPane = new HBox();
                autoArchiveDisplayBotPane.setMinHeight(2*TILE_SIZE);
                autoArchiveDisplayBotPane.setAlignment(Pos.CENTER);
                autoArchiveDisplayBotPane.setSpacing(6*TILE_SIZE);
                
                    VBox totalAutoArchiveResultBox = new VBox();
                    totalAutoArchiveResultBox.setSpacing(5);
                    totalAutoArchiveResultBox.setAlignment(Pos.CENTER_LEFT);
                    
                        HBox totalAutoArchiveResultSuccessBox = new HBox();
                        totalAutoArchiveResultSuccessBox.setSpacing(5);
                            Label totalAutoArchiveResultSuccess = new Label("Archives Successful:");
                            totalAutoArchiveResultSuccessCount = new Label(String.valueOf(autoArchiveSuccessCount));
                        totalAutoArchiveResultSuccessBox.getChildren().addAll(totalAutoArchiveResultSuccess, totalAutoArchiveResultSuccessCount);
                        
                        HBox totalAutoArchiveResultFailBox = new HBox();
                        totalAutoArchiveResultFailBox.setSpacing(5);
                            Label totalAutoArchiveResultFail = new Label("Archives Failed:");
                            totalAutoArchiveResultFailCount = new Label(String.valueOf(autoArchiveFailCount));
                        totalAutoArchiveResultFailBox.getChildren().addAll(totalAutoArchiveResultFail,totalAutoArchiveResultFailCount);
                        
                    totalAutoArchiveResultBox.getChildren().addAll(totalAutoArchiveResultSuccessBox,totalAutoArchiveResultFailBox);
                        
                    
                    VBox totalAutoArchiveUploadResultBox = new VBox();
                    totalAutoArchiveUploadResultBox.setSpacing(5);
                    totalAutoArchiveUploadResultBox.setAlignment(Pos.CENTER_LEFT);
                    
                        HBox totalAutoArchiveUploadResultSuccessBox = new HBox();
                        totalAutoArchiveUploadResultSuccessBox.setSpacing(5); 
                            Label totalAutoArchiveUploadResultSuccess = new Label("Uploads Successful:");
                            totalAutoArchiveUploadResultSuccessCount = new Label(String.valueOf(autoArchiveUploadSuccessCount));
                        totalAutoArchiveUploadResultSuccessBox.getChildren().addAll(totalAutoArchiveUploadResultSuccess,totalAutoArchiveUploadResultSuccessCount);
                        
                        HBox totalAutoArchiveUploadResultFailBox = new HBox();
                        totalAutoArchiveUploadResultFailBox.setSpacing(5); 
                            Label totalAutoArchiveUploadResultFail = new Label("Uploads Failed:");
                            totalAutoArchiveUploadResultFailCount = new Label(String.valueOf(autoArchiveUploadFailCount));
                        totalAutoArchiveUploadResultFailBox.getChildren().addAll(totalAutoArchiveUploadResultFail,totalAutoArchiveUploadResultFailCount);
                        
                    totalAutoArchiveUploadResultBox.getChildren().addAll(totalAutoArchiveUploadResultSuccessBox,totalAutoArchiveUploadResultFailBox);
                        
                autoArchiveDisplayBotPane.getChildren().addAll(totalAutoArchiveResultBox, totalAutoArchiveUploadResultBox);
                //End Bottom Pane
            autoArchiveDisplayPane.setBottom(autoArchiveDisplayBotPane);
            
        Scene autoArchiveDisplayScene = new Scene(autoArchiveDisplayPane);
        autoArchiveStage.setTitle("GotRadio, LLC. - Auto Archive Display");
        autoArchiveStage.setScene(autoArchiveDisplayScene);
        autoArchiveStage.show();
        
    }
    
    private void exitProgram(WindowEvent event){
        exitProgramHelper(event);
    }
    
    private void exitProgram(ActionEvent event){
        exitProgramHelper(event);
    }
    
    private void exitProgramHelper(Event event){
        if(uploadStatus.getText().equalsIgnoreCase("In Progress..") || archiveRunning || autoArchiveRunning){
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.getDialogPane().setMinSize(600, 200);
            alert.setTitle("GRDatabaseManager: ARCHIVE/UPLOAD IN PROGRESS!");
            alert.setContentText("Warning: Exiting application will terminate archive process."
                    + " Saving any pending uploads to the TextFile 'Cancelled Upload List' within the Local"
                    + " Archived folder.\n\nPress Cancel to continue operation.");

            alert.getButtonTypes().remove(ButtonType.OK);
            alert.getButtonTypes().add(ButtonType.CLOSE);
            Optional<ButtonType> result = alert.showAndWait();
            
            if(result.get().equals(ButtonType.CLOSE)){
                if(autoArchiveRunning){
                    autoArchiveCheckBox.setSelected(false);
                    Alert closingAlert = new Alert(AlertType.INFORMATION);
                    closingAlert.setTitle("GRDatabaseManager: Application closing...");
                    closingAlert.setContentText("GRDatabaseManager: Finishing Auto Archive Upload... Program will exit upon completion.");
                    closingAlert.show();
                }
                while(autoArchiveRunning){
                    //Wait for Upload to Finish...
                }
                Platform.exit();
                System.exit(0);
            } else {
                System.out.println("Resuming operation.");
                event.consume();
            }
        } else {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("GRDatabaseManager: Exit Confirmation.");
            alert.setContentText("Would you like to continue exiting the GRDatabaseManager?");

            alert.getButtonTypes().remove(ButtonType.OK);
            alert.getButtonTypes().add(ButtonType.CLOSE);
            Optional<ButtonType> result = alert.showAndWait();
            if(result.get().equals(ButtonType.CLOSE)){
                Platform.exit();
                System.exit(0);
            } else {
                event.consume();
            }
            
            
            
        }
    }
}


