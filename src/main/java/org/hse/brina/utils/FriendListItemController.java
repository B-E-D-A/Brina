package org.hse.brina.utils;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Класс FriendListItemController управляет отображением и настройкой элементов списка друзей.
 */

public class FriendListItemController implements Initializable {
    private static final Logger logger = LogManager.getLogger();
    @FXML
    public CheckBox writerAccessCheckBox;
    @FXML
    public CheckBox readerAccessCheckBox;
    @FXML
    public Text friendName;
    @FXML
    public HBox nameHBox;
    @FXML
    public HBox globalHBox;
    @FXML
    public Separator sep1;
    @FXML
    public Separator sep2;
    private Friend parent;

    public void setData(Friend friend, boolean isCheckBoxNeeded) {
        friendName.setText(friend.getName());
        HBox.setHgrow(nameHBox, Priority.ALWAYS);
        HBox.setHgrow(globalHBox, Priority.ALWAYS);
        parent = friend;
        if (!isCheckBoxNeeded) {
            readerAccessCheckBox.setVisible(false);
            writerAccessCheckBox.setVisible(false);
            sep1.setVisible(false);
            sep2.setVisible(false);
        } else {
            readerAccessCheckBox.setVisible(true);
            writerAccessCheckBox.setVisible(true);
            sep1.setVisible(true);
            sep2.setVisible(true);
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        readerAccessCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            parent.setReaderCheckBox(newValue);
        });
        writerAccessCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            parent.setWriterCheckBox(newValue);
        });

    }
}
