package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.Main;
import org.hse.brina.richtext.RichTextDemo;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * MainWindowController управляет главным окном приложения,
 * отображает документы, доступные пользователю.
 */
public class MainWindowController implements Initializable {
    private static final Logger logger = LogManager.getLogger();
    @FXML
    public HBox imageHBox;
    @FXML
    public Button logOutButton;
    @FXML
    public Button friends;
    @FXML
    private Button collab;
    @FXML
    private Button create_new;
    @FXML
    private Button my;

    private void loadScene(Stage stage, String fxmlView) throws IOException {
        if (fxmlView.equals(Config.getPathToViews() + "friends-view.fxml")) {
            Config.oldScene.replace(0, Config.oldScene.length(), Config.getPathToViews() + "main-window-view.fxml");
        }
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlView));
        Parent sceneLoader = loader.load();
        Scene scene;
        if (!fxmlView.equals(Config.getPathToViews() + "sign-in-view.fxml")) {
            scene = new Scene(sceneLoader, stage.getScene().getWidth(), stage.getScene().getHeight());
        } else {
            scene = new Scene(sceneLoader, Config.getDefaultWidth(), Config.getDefaultHeight());
        }

        stage.setScene(scene);
    }

    @FXML
    private void openCreateButton() {
        Stage stage = (Stage) create_new.getScene().getWindow();
        RichTextDemo richTextWindow = new RichTextDemo();
        richTextWindow.previousView = Config.getPathToViews() + "main-window-view.fxml";
        richTextWindow.start(stage);
    }

    @FXML
    private void openCollabButton() {
        Stage stage = (Stage) collab.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "collaboration-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    @FXML
    private void openMyProdButton() {
        Stage stage = (Stage) my.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "projects-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. here" + e.getMessage());
        }
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HBox.setHgrow(imageHBox, Priority.ALWAYS);
    }

    public void logOut(ActionEvent actionEvent) {
        Stage stage = (Stage) logOutButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "sign-in-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    public void openFriendList(ActionEvent actionEvent) {
        Stage stage = (Stage) friends.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "friends-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. here" + e.getMessage());
        }
    }
}
