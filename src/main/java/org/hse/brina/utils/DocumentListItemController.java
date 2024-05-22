package org.hse.brina.utils;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import org.hse.brina.Config;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class DocumentListItemController implements Initializable {
    @FXML
    public ImageView statusImage;
    @FXML
    public Text documentName;
    @FXML
    public HBox nameHBox;
    @FXML
    public HBox globalHBox;
    @FXML
    public Text accessText;

    public void setData(Document document) {
        documentName.setText(document.getName());
        HBox.setHgrow(nameHBox, Priority.ALWAYS);
        HBox.setHgrow(globalHBox, Priority.ALWAYS);
        Config.client.sendMessage("getLock " + documentName.getText());
        String response = Config.client.receiveMessage();
        if (response.equals("0")) {
            document.setStatus("unlocked");
            Image lockedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(Config.getPathToAssets()+"unlocked.png")));
            statusImage.setImage(lockedImage);
        } else {
            document.setStatus("locked");
            Image lockedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(Config.getPathToAssets()+"locked.png")));
            statusImage.setImage(lockedImage);
        }
        if (document.getAccess().equals("r")) {
            accessText.setText("reader");
        } else{
            accessText.setText("writer");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
