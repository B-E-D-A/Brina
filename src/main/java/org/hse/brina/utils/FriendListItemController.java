package org.hse.brina.utils;

/**
 * Класс FriendListItemController управляет отображением и настройкой элементов списка друзей.
 */

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import org.hse.brina.Config;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FriendListItemController implements Initializable {
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

    private static final Logger logger = LogManager.getLogger();
    private Friend parent;
    @FXML
    public Separator sep1;
    @FXML
    public Separator sep2;


    public void setData(Friend friend) {
        friendName.setText(friend.getName());
        HBox.setHgrow(nameHBox, Priority.ALWAYS);
        HBox.setHgrow(globalHBox, Priority.ALWAYS);
        parent = friend;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Old scene: " + Config.oldScene);
        if (Config.oldScene.equals("main-window")) {
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
        readerAccessCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            parent.setReaderCheckBox(newValue);
        });
        writerAccessCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            parent.setWriterCheckBox(newValue);
        });

    }
}
