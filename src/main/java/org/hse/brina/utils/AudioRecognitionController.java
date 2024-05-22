package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.richtext.RichTextDemo;
import org.hse.brina.speech.recognition.SpeechRecognition;

import java.io.File;

/**
 * Класс AudioRecognitionController отвечает за работу с пользовательским интерфейсом
 * и логику для функций распознавания речи в текст.
 */

public class AudioRecognitionController {
    private static final Logger logger = LogManager.getLogger();
    public Button loadAudio;
    public Button speechRecognitionButton;
    public TextArea resultArea;
    public Button Paste;
    public HBox pasteHBox;
    public VBox textVBox;
    public Text audioName;
    public Button recordAudioButton;
    public HBox audioHBox;
    public RichTextDemo.FoldableStyledArea documentArea;
    private StringBuilder path;

    @FXML
    public void initialize() {
        HBox.setHgrow(pasteHBox, Priority.ALWAYS);
        HBox.setHgrow(resultArea, Priority.ALWAYS);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        VBox.setVgrow(textVBox, Priority.ALWAYS);
        path = new StringBuilder();
        resultArea.setWrapText(true);
        audioName.setVisible(false);
    }

    public void loadAudio(ActionEvent actionEvent) {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load audio");
        fileChooser.setInitialDirectory(new File(initialDir));

        FileChooser.ExtensionFilter mp3Extension = new FileChooser.ExtensionFilter("MP3 Audio (*.mp3)", "*.mp3");
        fileChooser.getExtensionFilters().addAll(mp3Extension);

        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        File selectedFile = fileChooser.showOpenDialog(resultArea.getScene().getWindow());
        if (selectedFile != null) {
            audioName.setVisible(true);
            path.replace(0, path.length(), selectedFile.getAbsolutePath());
            audioName.setText(" " + selectedFile.getName());
        }
    }

    public void recognizeSpeech(ActionEvent actionEvent) throws Exception {
        SpeechRecognition recognizer = new SpeechRecognition();
        if (!path.isEmpty()) {
            String content = recognizer.translateAudioToText(path.toString());
            resultArea.setText(content);
        }
    }

    public void pasteResult(ActionEvent actionEvent) {
        try {
            documentArea.insertText(documentArea.getCaretPosition(), resultArea.getText());
            Stage stage = (Stage) speechRecognitionButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.error("RichText document is not created so recognized text could not be pasted");
        }
    }

    public void recordAudio(ActionEvent actionEvent) {
    }

}
