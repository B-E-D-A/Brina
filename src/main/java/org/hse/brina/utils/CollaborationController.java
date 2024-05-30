package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.Main;
import org.hse.brina.richtext.RichTextDemo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Класс CollaborationController отвечает за открытие документа по его идентификатору для совместной работы над ним.
 */

public class CollaborationController {

    private static final Logger logger = LogManager.getLogger();
    public Button Enter;
    @FXML
    public TextField EnterIdField;
    @FXML
    public Button backButton;
    @FXML
    public VBox globalVBox;
    @FXML
    public VBox IDVBox;
    public Button friendsButton;


    private void loadScene(Stage stage, String fxmlView) throws IOException {
        if(fxmlView.equals(Config.getPathToViews()+"friends-view.fxml")){
            logger.info("FROM COLLAB PAGE");
            Config.oldScene.replace(0, Config.oldScene.length(),Config.getPathToViews()+"collaboration-view.fxml");
        }
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlView));
        logger.info("LOAD: " + fxmlView);
        Parent logInLoader = loader.load();
        Scene scene = new Scene(logInLoader, stage.getScene().getWidth(), stage.getScene().getHeight());
        logger.info("SCENE CREATED");
        stage.setScene(scene);
    }


    @FXML
    private void openEnterButton() {
        String id = EnterIdField.getText();
        Config.client.sendMessage("openDocumentById " + Config.client.getName() + " " + id);
        String response = Config.client.receiveMessage();
        if (response.equals("No such file")) {
            Stage popupStage = new Stage();
            popupStage.initOwner(Enter.getScene().getWindow());
            popupStage.initModality(Modality.APPLICATION_MODAL);
            int stageHeight = 100;
            int stageWidth = 200;
            VBox vbox = new VBox();
            Text text = new Text("No such file is found");
            text.setStyle("-fx-font-size: 16 px");
            text.setFill(Color.BLACK);
            text.setTextAlignment(TextAlignment.CENTER);
            vbox.setAlignment(Pos.CENTER);
            vbox.getChildren().addAll(text);
            vbox.setStyle("-fx-background-color: white;");
            vbox.setPrefWidth(200);
            vbox.setPrefHeight(100);
            vbox.setMaxWidth(200);
            vbox.setMaxHeight(100);

            Scene scene = new Scene(vbox, stageWidth, stageHeight);
            popupStage.setScene(scene);
            if (Enter.getScene() != null) {
                Window previousWindow = Enter.getScene().getWindow();
                double centerX = previousWindow.getX() + (previousWindow.getWidth() - stageWidth) / 2;
                double centerY = previousWindow.getY() + (previousWindow.getHeight() - stageHeight) / 2;
                popupStage.setX(centerX);
                popupStage.setY(centerY);
            }
            try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
                Image icon = new Image(iconStream);
                popupStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            popupStage.setResizable(false);
            popupStage.showAndWait();
        } else {
            String[] responseData = response.split(" ");
            String filePath = responseData[0];
            Config.client.sendMessage("lockDocument " + id);
            RichTextDemo richTextWindow = new RichTextDemo();
            richTextWindow.previousView = Config.getPathToViews() + "collaboration-view.fxml";
            richTextWindow.start((Stage) Enter.getScene().getWindow());
            File file = new File(filePath);
            if (file.exists()) richTextWindow.loadRTFX(file);
        }
    }

    @FXML
    public void backButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "main-window-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    @FXML
    void initialize() {
        HBox.setHgrow(globalVBox, Priority.ALWAYS);
        VBox.setVgrow(globalVBox, Priority.ALWAYS);
        HBox.setHgrow(IDVBox, Priority.ALWAYS);
        VBox.setVgrow(IDVBox, Priority.ALWAYS);
    }

    public void friendsButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) friendsButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "friends-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

}
