package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.yandexGPT.server.GPTServer;

public class YandexGptController {
    private static final Logger logger = LogManager.getLogger();
    @FXML
    public TextArea gptTextArea;
    @FXML
    public HBox hBox;
    @FXML
    public VBox vBox;
    @FXML
    public Button sendButton;
    @FXML
    public TextArea userTextArea;

    @FXML
    public void initialize() {
        HBox.setHgrow(hBox, Priority.ALWAYS);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        VBox.setVgrow(hBox, Priority.ALWAYS);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        HBox.setHgrow(gptTextArea, Priority.ALWAYS);
        VBox.setVgrow(gptTextArea, Priority.ALWAYS);
        HBox.setHgrow(userTextArea, Priority.ALWAYS);
        VBox.setVgrow(userTextArea, Priority.ALWAYS);
        userTextArea.setWrapText(true);
        gptTextArea.setWrapText(true);
    }

    public void sendButtonClicked() {
        try {
            if (!userTextArea.getText().isEmpty()) {
                String gptResult = GPTServer.getGPTProcessing("Ответь на вопрос ", userTextArea.getText());
                gptTextArea.setText(gptResult);
            }
        } catch (Exception e) {
            logger.info("Error while making request to Yandex GPT");
        }
    }
}
