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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.Main;
import org.hse.brina.richtext.RichTextDemo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Класс ProjectsController отвечает за выгрузку документов пользователя,
 * управление представлением списка документов в приложении.
*/

public class ProjectsController implements Initializable {
    private static final Logger logger = LogManager.getLogger();
    @FXML
    public VBox documentList;
    @FXML
    public Button backButton;
    @FXML
    public HBox labelsHBox;
    @FXML
    public VBox documentVBox;
    @FXML
    public VBox globalVBox;
    @FXML
    public HBox gHBox;
    private Map<String, String> userDocumentsMap;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HBox.setHgrow(labelsHBox, Priority.ALWAYS);
        HBox.setHgrow(globalVBox, Priority.ALWAYS);
        VBox.setVgrow(documentVBox, Priority.ALWAYS);
        VBox.setVgrow(documentList, Priority.ALWAYS);
        VBox.setVgrow(globalVBox, Priority.ALWAYS);
        HBox.setHgrow(gHBox, Priority.ALWAYS);
        List<Document> documents = new ArrayList<>(documents());
        for (Document document : documents) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Config.getPathToViews()+"document-list-item-view.fxml"));
            try {
                HBox documentHBox = loader.load();
                DocumentListItemController controller = loader.getController();
                controller.setData(document);

                controller.nameHBox.setOnMouseClicked(event -> {
                   String[] filename = controller.documentName.getText().split("\\.");
                    Config.client.sendMessage("lockDocument " + filename[0].hashCode());
                    String response = Config.client.receiveMessage();
                    String key = controller.documentName.getText();
                    String value = userDocumentsMap.get(controller.accessText.getText().charAt(0)+key);
                    if (!response.equals("Document closed") && key != null && value != null && !(Objects.equals(key, " ") || key.isEmpty()) && !(Objects.equals(value, " ") || value.isEmpty())) {
                        RichTextDemo richTextWindow = new RichTextDemo();
                        richTextWindow.previousView = Config.getPathToViews()+"projects-view.fxml";
                        richTextWindow.start((Stage) documentList.getScene().getWindow());
                        File file = new File(value);
                        if (file.exists()) {
                            richTextWindow.loadRTFX(file);
                        }
                    }
                });
                documentList.getChildren().add(documentHBox);
            } catch (IOException e) {
                logger.error("Error while loading elements" + e.getMessage());
            }
        }
    }

    public List<Document> documents() {
        List<Document> documentsList = new ArrayList<>();
        String username = Config.client.getName();
        Config.client.sendMessage("getDocuments " + username);
        String response = Config.client.receiveMessage();
        userDocumentsMap = new LinkedHashMap<>();
        String[] pairs = response.split(" ");
        for (int i = 0; i < pairs.length - 1; i += 2) {
            userDocumentsMap.put(pairs[i], pairs[i + 1]);
        }
        ArrayList<String> keySet = new ArrayList<>(userDocumentsMap.keySet());
        for (String s : keySet) {
            Document document = new Document(s.substring(0, 1), s.substring(1), Document.STATUS.UNLOCKED);
            documentsList.add(document);
        }
        return documentsList;
    }

    public void backButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews()+"main-window-view.fxml");
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    private void loadScene(Stage stage, String fxmlView) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlView));
        Parent logInLoader = loader.load();
        Scene scene = new Scene(logInLoader, stage.getScene().getWidth(), stage.getScene().getHeight());
        stage.setScene(scene);
    }
}
