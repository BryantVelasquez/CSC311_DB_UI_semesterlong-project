package viewmodel;

import dao.DbConnectivityClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;




public class SignUpController {
    @FXML
    private TextField UserNameTXT;
    @FXML
    private PasswordField passWordTXT;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    public void createNewAccount(ActionEvent actionEvent) {
        boolean x = cnUtil.createStuff(UserNameTXT.getText(),passWordTXT.getText());
        if (x == true) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Account Created");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Account error! Account already exists!");
            alert.showAndWait();
        }
    }

    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
