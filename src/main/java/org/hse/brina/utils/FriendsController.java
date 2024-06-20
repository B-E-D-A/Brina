package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;
import org.hse.brina.richtext.RichTextDemo;
import org.hse.brina.signin.SignInController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Класс FriendsController управляет интерфейсом списка друзей, загружая и отображая
 * список друзей пользователя, а также обеспечивая добавление новых друзей.
 */
public class FriendsController implements Initializable {

    private static final Logger logger = LogManager.getLogger();
    private final List<Friend> friendsList = new ArrayList<Friend>();
    private final Stage popupStage = new Stage();
    @FXML
    public VBox globalVBox;
    @FXML
    public HBox gHBox;
    @FXML
    public HBox labelsHBox;
    @FXML
    public Button backButton;
    @FXML
    public VBox friendsVBox;
    @FXML
    public VBox friendList;
    public Button actionButton;
    @FXML
    public Text heading;
    public Text friendNotFoundWarning = new Text();
    public Text noFriendsWarning = new Text();
    @FXML
    public HBox headingHBox;
    @FXML
    public HBox buttonsHBox;
    @FXML
    public HBox readerHeading;
    @FXML
    public HBox writerHeading;
    TextField infoTextField = new TextField();

    public void backButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        try {
            SignInController.loadScene(stage, Config.oldScene.toString(), stage.getScene().getWidth(), stage.getScene().getHeight());
        } catch (IOException e) {
            logger.error("Scene configuration file not found. " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HBox.setHgrow(labelsHBox, Priority.ALWAYS);
        HBox.setHgrow(globalVBox, Priority.ALWAYS);
        VBox.setVgrow(friendsVBox, Priority.ALWAYS);
        VBox.setVgrow(friendList, Priority.ALWAYS);
        VBox.setVgrow(globalVBox, Priority.ALWAYS);
        HBox.setHgrow(gHBox, Priority.ALWAYS);
        HBox.setHgrow(headingHBox, Priority.ALWAYS);
        boolean isCheckBoxNeeded = !Config.oldScene.toString().equals(Config.getPathToViews() + "main-window-view.fxml");
        if (isCheckBoxNeeded) {
            noFriendsWarning.setText("Выберите друзей для совместной работы");
            noFriendsWarning.setVisible(false);
            noFriendsWarning.getStyleClass().add("warning-text");
            Button createJointProjectButton = new Button("Создать");
            createJointProjectButton.setAlignment(Pos.CENTER);
            createJointProjectButton.setOnAction(event -> createJointProject());
            createJointProjectButton.getStyleClass().add("recognize-button");
            createJointProjectButton.setStyle("-fx-text-fill: #434c55ff");
            createJointProjectButton.setPrefWidth(80);

            HBox createHBox = new HBox();
            createHBox.setAlignment(Pos.CENTER_RIGHT);
            createHBox.setSpacing(10);
            createHBox.setPrefWidth(110);
            createHBox.getChildren().addAll(noFriendsWarning, createJointProjectButton);

            VBox vbox = new VBox(createHBox);
            vbox.setPrefWidth(520);
            vbox.setPrefHeight(25);
            vbox.setAlignment(Pos.CENTER_RIGHT);
            vbox.setPadding(new Insets(10, 10, 0, 0));

            HBox newHBox = new HBox(buttonsHBox, vbox);
            newHBox.setPadding(new Insets(0, 0, 7, 0));
            labelsHBox.getChildren().remove(buttonsHBox);
            globalVBox.getChildren().add(0, newHBox);
            Separator separator = new Separator();
            separator.setPrefWidth(200.0);
            globalVBox.getChildren().add(1, separator);
        } else {
            labelsHBox.getChildren().removeAll(readerHeading, writerHeading);
        }
        friendNotFoundWarning.setText("Пользователь " + infoTextField.getText() + " не найден");
        friendNotFoundWarning.setVisible(false);

        Config.client.sendMessage("getFriendsList " + Config.client.getName());
        String response = Config.client.receiveMessage();
        if (!response.equals("No friends found")) {
            List<Friend> friends = getFriends(response);
            for (Friend friend : friends) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Config.getPathToViews() + "friend-list-item-view.fxml"));
                try {
                    HBox friendHBox = loader.load();
                    FriendListItemController controller = loader.getController();
                    controller.setData(friend, isCheckBoxNeeded);
                    friendList.getChildren().add(friendHBox);
                } catch (IOException e) {
                    logger.error("Error while loading elements" + e.getMessage());
                }
            }
        }
    }

    private void showPopupWindow(String text, String buttonText, Runnable action) {
        Stage oldStage = (Stage) actionButton.getScene().getWindow();
        popupStage.initOwner(oldStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        Text infoText = new Text(text);
        infoText.getStyleClass().add("simple-text");
        infoTextField.setPrefHeight(40);
        infoTextField.setPrefWidth(100);
        infoTextField.getStyleClass().add("login-field");
        Button continueActionButton = new Button();
        continueActionButton.setText(buttonText);
        continueActionButton.setOnAction(event -> action.run());
        continueActionButton.getStyleClass().add("file-button");

        HBox hbox = new HBox();
        hbox.getChildren().addAll(infoText, infoTextField, continueActionButton);
        hbox.setAlignment(Pos.CENTER);
        hbox.setSpacing(10);
        hbox.setMaxWidth(300);
        hbox.setStyle("-fx-background-color: #fffcf7ff");
        friendNotFoundWarning.getStyleClass().add("warning-text");
        VBox popupLayout = new VBox(hbox, friendNotFoundWarning);
        popupLayout.setAlignment(Pos.CENTER);
        popupLayout.setSpacing(10);
        popupLayout.setStyle("-fx-background-color: #fffcf7ff");
        popupLayout.setPadding(new Insets(10));
        int stageHeight = 90;
        int stageWidth = 320;
        Scene popupScene = new Scene(popupLayout, stageWidth, stageHeight);
        popupScene.getStylesheets().add(Config.getPathToCss() + "main-page-style.css");
        popupStage.setScene(popupScene);
        popupStage.centerOnScreen();
        try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
            Image icon = new Image(iconStream);
            popupStage.getIcons().add(icon);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        popupStage.setResizable(false);
        popupStage.showAndWait();
    }

    public void createJointProject() {
        int count = 0;
        for (Friend friend : friendsList) {
            if (!friend.getAccess().equals("none")) {
                count++;
            }
        }
        if (count == 0) {
            noFriendsWarning.setVisible(true);
        } else {
            noFriendsWarning.setVisible(false);
            Runnable action = () -> {
                popupStage.close();
                Config.client.sendMessage("addDocumentById " + Config.client.getName() + " " + infoTextField.getText() + " w");
                for (Friend friend : friendsList) {
                    if (friend.getAccess().equals("w")) {
                        Config.client.sendMessage("addDocumentById " + friend.getName() + " " + infoTextField.getText() + " " + friend.getAccess());
                    } else if (friend.getAccess().equals("r")) {
                        Config.client.sendMessage("addDocumentById " + friend.getName() + " " + infoTextField.getText() + " " + friend.getAccess());
                    }
                }
                RichTextDemo richTextWindow = new RichTextDemo();
                richTextWindow.previousView = Config.getPathToViews() + "friends-view.fxml";
                richTextWindow.start((Stage) friendList.getScene().getWindow());
                richTextWindow.documentNameField.setText(infoTextField.getText());
            };
            showPopupWindow("Название документа", "Создать", action);
        }
    }

    public void addFriend(ActionEvent actionEvent) {
        Runnable action = () -> {
            Config.client.sendMessage("addFriend " + Config.client.getName() + " " + infoTextField.getText());
            String response = Config.client.receiveMessage();
            boolean isSuccessful = response.equals("Friend added successfully");
            friendNotFoundWarning.setVisible(!isSuccessful);
            if (isSuccessful) {
                popupStage.close();
            }
        };
        showPopupWindow("Имя друга:", "Добавить", action);
    }


    private List<Friend> getFriends(String friends) {
        String[] friendsArr = friends.split(" ");
        for (String friendName : friendsArr) {
            Friend friend = new Friend("none", friendName);
            friendsList.add(friend);
        }
        return friendsList;
    }
}
