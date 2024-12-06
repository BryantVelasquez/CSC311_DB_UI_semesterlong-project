package viewmodel;

import com.azure.storage.blob.BlobClient;
import dao.DbConnectivityClass;
//import dao.StorageUploader;
import dao.StorageUploader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DB_GUI_Controller implements Initializable {

    StorageUploader store = new StorageUploader();

    @FXML
    ProgressBar progressBar;
    @FXML
    TextField first_name, last_name, department, major, email, imageURL;

    @FXML
    private Label statusLabel;

    @FXML
    ComboBox<Major> majorComboBox;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;

    @FXML
    private Button editButton, addButton, deleteButton, clearButton;

    private final String firstNameRegex = "^[A-Z][a-z]+";
    private final String lastNameRegex = "^[A-Z][a-z]+";

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();





    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            //Populates the combobox with the Major enum values
            majorComboBox.setItems(FXCollections.observableArrayList(Major.values()));
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    protected void addNewRecord() {
        if (regex(firstNameRegex, first_name.getText()) &&
                regex(lastNameRegex, last_name.getText()) &&
                regex("([A-Za-z0-9]+)(@)([A-Za-z0-9]+)(.)([A-Za-z0-9]+)", email.getText())) {

            Major selectedMajor = majorComboBox.getValue();

            if (selectedMajor != null) {
                Person p = new Person(
                        first_name.getText(),
                        last_name.getText(),
                        department.getText(),
                        selectedMajor.toString(),
                        email.getText(),
                        imageURL.getText()
                );

                try {
                    cnUtil.insertUser(p);
                    cnUtil.retrieveId(p);
                    p.setId(cnUtil.retrieveId(p));
                    data.add(p);
                    clearForm();

                    updateStatus("Record added successfully.");
                } catch (Exception e) {
                    updateStatus("Error adding record: " + e.getMessage());
                }
            } else {
                updateStatus("Please select a Major.");
            }
        } else {
            updateStatus("Invalid input. Please check the fields.");
        }
    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
       // majorComboBox.setValue(Major.valueOf(""));
        email.setText("");
        imageURL.setText("");

        majorComboBox.getSelectionModel().clearSelection(); //clears the combobox selection
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    protected void displayHelp(){
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/help.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            try {
                int index = data.indexOf(p);
                Person p2 = new Person(index + 1, first_name.getText(), last_name.getText(), department.getText(),
                        majorComboBox.getValue() != null ? majorComboBox.getValue().toString() : major.getText(),
                        email.getText(), imageURL.getText());
                cnUtil.editUser(p.getId(), p2);
                data.set(index, p2);

                updateStatus("Record updated successfully.");
            } catch (Exception e) {
                updateStatus("Error updating record: " + e.getMessage());
            }
        } else {
            updateStatus("No record selected to edit.");
        }

    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            try {
                cnUtil.deleteRecord(p);
                data.remove(p);
                updateStatus("Record deleted successfully.");
            } catch (Exception e) {
                updateStatus("Error deleting record: " + e.getMessage());
            }
        } else {
            updateStatus("No record selected to delete.");
        }

    }
    private Task<Void> createUploadTask(File file, ProgressBar progressBar) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                BlobClient blobClient = store.getContainerClient().getBlobClient(file.getName());
                long fileSize = Files.size(file.toPath());
                long uploadedBytes = 0;

                try (FileInputStream fileInputStream = new FileInputStream(file);
                     OutputStream blobOutputStream = blobClient.getBlockBlobClient().getBlobOutputStream()) {

                    byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer size
                    int bytesRead;

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        blobOutputStream.write(buffer, 0, bytesRead);
                        uploadedBytes += bytesRead;

                        // Calculate and update progress as a percentage
                        int progress = (int) ((double) uploadedBytes / fileSize * 100);
                        updateProgress(progress, 100);
                    }
                }

                return null;
            }
        };
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));

            Task<Void> uploadTask = createUploadTask(file, progressBar);
            progressBar.progressProperty().bind(uploadTask.progressProperty());
            new Thread(uploadTask).start();
        }
    }

    @FXML
    protected void addRecord() {
            showSomeone();
        }

    private boolean regex(String regex, String string){

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);
        boolean match = false;
        while(matcher.find()){
            match = true;
            } return match;
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if(p != null) {
            editButton.setDisable(false);
            deleteButton.setDisable(false);
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());
            majorComboBox.setValue(Major.valueOf(p.getMajor()));
            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());

            //sets the combobox to its correct value
            try{
                majorComboBox.setValue(Major.valueOf(p.getMajor()));
            }catch (IllegalArgumentException e){
                System.out.println("Invalid major value: " + p.getMajor());
                majorComboBox.getSelectionModel().clearSelection();
            }
        }



    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2,textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private static enum Major {
        Business, CSC, CPIS, English, Biology //added new enum values :D
    }

    private void updateStatus(String message){
        Platform.runLater(() -> statusLabel.setText(message));
    }



    private static class Results {

        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

    @FXML
    protected void importCSV() throws FileNotFoundException{
        System.out.println("importCSV");
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        Scanner sc = new Scanner(file);
        sc.nextLine();
        try{
            while(sc.hasNextLine()){
                String li = sc.nextLine();
                if(!li.isEmpty()){
                    String[] part = li.split(",");
                    cnUtil.insertUser(new Person(part[0], part[1], part[2], part[3], part[4], ""));
                }
            }
            updateStatus("CSV imported successfully");
        } catch (Exception e) {
            updateStatus("Error importing CSV");
        }
        sc.close();
        tv.setItems(cnUtil.getData());
    }

    @FXML
    protected void exportCSV() throws IOException {
        System.out.println("Export CSV");
        FileWriter fw = new FileWriter("src/main/resources/ExportCSVTest");
        File file = new File("src/main/resources/ExportCSVTest");
        file.createNewFile();

        fw.write("firstname,lastname,department,major,email\n");
        fw.write(cnUtil.stringAllUsers());

        statusLabel.setText("Exported to " + file.getAbsolutePath());


        fw.close();

    }



}