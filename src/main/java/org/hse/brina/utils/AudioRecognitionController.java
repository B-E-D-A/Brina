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
import org.hse.brina.Config;
import org.hse.brina.richtext.RichTextDemo;
import org.hse.brina.speech.recognition.AudioRecorder;
import org.hse.brina.speech.recognition.ConverterToMP3;
import org.hse.brina.speech.recognition.SpeechRecognition;

import java.io.File;

/**
 * Класс AudioRecognitionController отвечает за работу с пользовательским интерфейсом
 * и логику для функций распознавания речи в текст.
 */

public class AudioRecognitionController {
    private static final Logger logger = LogManager.getLogger();
    private final AudioRecorder recorder = new AudioRecorder();
    @FXML
    public Button loadAudio;
    @FXML
    public Button speechRecognitionButton;
    @FXML
    public TextArea resultArea;
    @FXML
    public Button Paste;
    @FXML
    public HBox pasteHBox;
    @FXML
    public VBox textVBox;
    @FXML
    public Text audioName;
    @FXML
    public Button recordAudioButton;
    @FXML
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

    @FXML
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

    @FXML
    public void recognizeSpeech(ActionEvent actionEvent) throws Exception {
        ConverterToMP3 converter = new ConverterToMP3();
        converter.convertWAVToMP3();
        path.replace(0, path.length(), Config.getProjectPath() + "\\result.mp3");
        SpeechRecognition recognizer = new SpeechRecognition();
        if (!path.isEmpty()) {
            String content = recognizer.translateAudioToText(path.toString());
            resultArea.setText(content);
        }
    }

    @FXML
    public void pasteResult(ActionEvent actionEvent) {
        try {
            documentArea.insertText(documentArea.getCaretPosition(), resultArea.getText());
            Stage stage = (Stage) speechRecognitionButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.error("RichText document is not created so recognized text could not be pasted");
        }
    }

    @FXML
    public void recordAudio(ActionEvent actionEvent) {
        if (recordAudioButton.getText().equals("Записать")) {
            recorder.startRecording();
            recordAudioButton.setText("Остановить запись");
        } else if (recordAudioButton.getText().equals("Остановить запись")) {
            recorder.stopRecording();
            recordAudioButton.setText("Записать");
            path.replace(0, path.length(), Config.getProjectPath() + "\\record.wav");
        }
    }

}