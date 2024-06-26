package org.hse.brina.signin;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * SignInController управляет окно входа в приложение,
 * проверяет соответствие логина и пароля уже зарегистрированного пользователя,
 * отображает ошибки при некорректном введении данных,
 * осуществляет переход на главное окно приложения при успешной авторизации.
 */
public class SignInController {

    private static final Logger logger = LogManager.getLogger();
    @FXML
    protected Label welcomeText;
    @FXML
    protected Button signInButton;
    @FXML
    protected Button signUpButton;
    @FXML
    protected TextField loginField;
    @FXML
    protected PasswordField passwordField;
    @FXML
    protected TextField invalidLoginField;
    @FXML
    protected TextField invalidPasswordField;
    @FXML
    private Button eyeButton;
    @FXML
    private TextField openedPasswordField;
    @FXML
    private ImageView eyeImage;
    @FXML
    private Image eyeOpenImage;
    @FXML
    private Image eyeClosedImage;
    private boolean passwordVisible = false;

    protected static String getHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.info("Unable to hash password");
            return null;
        }
    }

    public static void loadScene(Stage stage, String fxmlView, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlView));
        Parent logInLoader = loader.load();
        Scene scene = new Scene(logInLoader, width, height);
        stage.setScene(scene);
    }

    @FXML
    protected void initialize() {

        eyeOpenImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(Config.getPathToAssets() + "open-eye.png")));
        eyeClosedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(Config.getPathToAssets() + "closed-eye.png")));

        setEyeButtonAction();

        invalidLoginField.setOnMouseClicked(event -> hideWarningAboutEmptyField(invalidLoginField));
        invalidPasswordField.setOnMouseClicked(event -> hideWarningAboutEmptyField(invalidPasswordField));

    }

    protected boolean checkIfFieldsAreEmpty() {
        boolean isValid = true;
        eyeButton.setOnAction(Event::consume);
        if (loginField.getText().isEmpty()) {
            loginField.setVisible(false);
            invalidLoginField.setText("Введите свой логин");
            invalidLoginField.setVisible(true);
            isValid = false;
        } else {
            invalidLoginField.setVisible(false);
            loginField.setVisible(true);
        }
        if (passwordField.getText().isEmpty()) {
            passwordField.setVisible(false);
            invalidPasswordField.setText("Введите свой пароль");
            invalidPasswordField.setVisible(true);
            isValid = false;
        } else {
            invalidPasswordField.setVisible(false);
            passwordField.setVisible(true);
        }
        return isValid;
    }

    @FXML
    private void signInButtonClicked() {
        Stage stage = (Stage) signInButton.getScene().getWindow();
        boolean isValid = checkIfFieldsAreEmpty();
        if (!isValid) return;
        String username = loginField.getText();
        String password = passwordField.getText();
        Config.client.setName(username);
        Config.client.sendMessage("signInUser " + username + " " + getHash(password + password.hashCode()));
        String response = Config.client.receiveMessage();
        if (response.equals("User with this name not found")) {
            loginField.setVisible(false);
            invalidLoginField.setText("Пользователя с таким именем нет");
            invalidLoginField.setVisible(true);
            setEyeButtonAction();
        } else if (response.equals("Wrong password")) {
            passwordField.setVisible(false);
            invalidPasswordField.setText("Неверный пароль");
            invalidPasswordField.setVisible(true);
            setEyeButtonAction();
        } else if (isValid && response.equals("User logged in")) {
            loginField.setVisible(true);
            passwordField.setVisible(true);
            enter(stage);
        }
    }

    @FXML
    private void signUpFromInButtonClicked() {
        Stage stage = (Stage) signUpButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "sign-up-view.fxml", Config.getDefaultWidth(), Config.getDefaultHeight());
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    private void enter(Stage stage) {
        try {
            loadScene(stage, Config.getPathToViews() + "main-window-view.fxml", Config.getDefaultWidth(), Config.getDefaultHeight());
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
        stage.setResizable(true);
    }

    private void hideWarningAboutEmptyField(TextField field) {
        if (field.isVisible()) {
            field.setVisible(false);
            if (field.equals(invalidLoginField)) loginField.setVisible(true);
            if (field.equals(invalidPasswordField)) passwordField.setVisible(true);
        }
    }

    private void changePasswordVisibility(Image eyeClosed, TextField openedPasswordField, TextField passwordField) {
        String passwordOpen = openedPasswordField.getText();
        passwordField.setVisible(true);
        passwordField.requestFocus();
        passwordField.setText(passwordOpen);
        passwordField.positionCaret(passwordOpen.length());
        openedPasswordField.setVisible(false);
        eyeImage.setImage(eyeClosed);
        passwordVisible = !passwordVisible;
    }

    protected void setEyeButtonAction() {
        eyeButton.setOnAction(event -> {
            if (!passwordVisible) {
                changePasswordVisibility(eyeOpenImage, passwordField, openedPasswordField);
            } else {
                changePasswordVisibility(eyeClosedImage, openedPasswordField, passwordField);
            }
        });
    }
}