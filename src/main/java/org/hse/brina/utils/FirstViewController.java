package org.hse.brina.utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.signin.SignInController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FirstViewController показывает первое окно с названием приложения,
 * затем выполняет переход на окно входа в систему.
 */
public class FirstViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger();
    @FXML
    public Label text;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), text);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> openWelcomeWindow());

        FadeTransition delay = new FadeTransition(Duration.seconds(2), text);
        delay.setFromValue(1.0);
        delay.setToValue(1.0);
        delay.setOnFinished(event -> fadeOut.play());

        delay.play();
    }

    private void openWelcomeWindow() {
        Stage stage = (Stage) text.getScene().getWindow();
        try {
            SignInController.loadScene(stage, Config.getPathToViews() + "sign-in-view.fxml", Config.getDefaultWidth(), Config.getDefaultHeight());
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }
}