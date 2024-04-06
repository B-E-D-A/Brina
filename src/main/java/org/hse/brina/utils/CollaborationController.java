package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.Main;

import java.io.IOException;

public class CollaborationController {

    public Button Enter;
    @FXML
    public TextField EnterIdField;
    @FXML
    public Button backButton;
    private static final Logger logger = LogManager.getLogger();

    private void loadScene(Stage stage, String fxmlView) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlView));
        Parent logInLoader = loader.load();
        Scene scene = new Scene(logInLoader, stage.getScene().getWidth(), stage.getScene().getHeight());
        stage.setScene(scene);
    }

    @FXML
    private void openEnterButton() {
        Stage stage = (Stage) Enter.getScene().getWindow();
        try {
            loadScene(stage, "/org/hse/brina/views/collaboration-entered-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    @FXML
    public void backButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        try {
            loadScene(stage, "/org/hse/brina/views/main-window-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }
}
