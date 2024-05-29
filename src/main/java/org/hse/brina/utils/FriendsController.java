package org.hse.brina.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import org.hse.brina.Main;
import org.hse.brina.richtext.RichTextDemo;

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
    private List<Friend> friendsList = new ArrayList<Friend>();
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
    public Button addFriendButton;
    @FXML
    public Button continueButton;
    @FXML
    public Text heading;
    public Text warning = new Text();
    TextField friendNameTextField = new TextField();
    private final Stage popupStage = new Stage();

    public void backButtonClicked(ActionEvent actionEvent) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        try {
            loadScene(stage, Config.getPathToViews() + "main-window-view.fxml");
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HBox.setHgrow(labelsHBox, Priority.ALWAYS);
        HBox.setHgrow(globalVBox, Priority.ALWAYS);
        VBox.setVgrow(friendsVBox, Priority.ALWAYS);
        VBox.setVgrow(friendList, Priority.ALWAYS);
        VBox.setVgrow(globalVBox, Priority.ALWAYS);
        HBox.setHgrow(gHBox, Priority.ALWAYS);

        Config.client.sendMessage("getFriendsList " + Config.client.getName());
        String response = Config.client.receiveMessage();
        if (!response.equals("No friends found")) {
            List<Friend> friends = getFriends(response);

            for (Friend friend : friends) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Config.getPathToViews() + "friend-list-item-view.fxml"));
                try {
                    HBox friendHBox = loader.load();
                    FriendListItemController controller = loader.getController();
                    controller.setData(friend);
                    friendList.getChildren().add(friendHBox);
                } catch (IOException e) {
                    logger.error("Error while loading elements" + e.getMessage());
                }
            }
        }
    }

    public void addFriend(ActionEvent actionEvent) {
        Runnable action = () -> {
            Config.client.sendMessage("addFriend " + Config.client.getName() + " " + friendNameTextField.getText());
            String response = Config.client.receiveMessage();
            if (!response.equals("Friend added successfully")) {
                warning.setVisible(true);
            } else {
                warning.setVisible(false);
            }
        };
        showPopupWindow("Имя друга:", "Добавить", action);

    }

    private void showPopupWindow(String text, String buttonText, Runnable action) {
        Stage popupStage = new Stage();
        Stage oldStage = (Stage) addFriendButton.getScene().getWindow();
        popupStage.initOwner(oldStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        Text friendName = new Text(text);
        friendNameTextField.setMaxWidth(100);
        friendNameTextField.setMaxHeight(30);
        friendNameTextField.setPrefHeight(30);
        friendNameTextField.setPrefWidth(70);
        warning.setText("Пользователь " + friendNameTextField.getText() + " не найден");
        warning.setVisible(false);

        Button addFriend = new Button();
        addFriend.setText(buttonText);
        addFriend.setOnAction(event -> action.run());

        HBox nameHBox = new HBox();
        nameHBox.getChildren().addAll(friendName, friendNameTextField, addFriend);
        nameHBox.setAlignment(Pos.CENTER);
        nameHBox.setSpacing(10);
        nameHBox.setMaxWidth(300);

        VBox popupLayout = new VBox(nameHBox);
        popupLayout.setAlignment(Pos.CENTER);
        popupLayout.getStyleClass().add("white-box");
        popupLayout.setSpacing(10);
        int stageHeight = 100;
        int stageWidth = 310;
        Scene popupScene = new Scene(popupLayout, stageWidth, stageHeight);
        popupScene.getStylesheets().add(Config.getPathToCss() + "sign-in-page-style.css");
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

    public void continueCollabing(ActionEvent actionEvent) {

        Runnable action = () -> {
            if (popupStage != null) popupStage.close();
            Config.client.sendMessage("addDocumentById " + Config.client.getName() + " " + friendNameTextField.getText() + " w");
            for (Friend friend : friendsList) {
                if (friend.getAccess().equals("w")) {
                    Config.client.sendMessage("addDocumentById " + friend.getName() + " " + friendNameTextField.getText() + " " + friend.getAccess());
                } else if (friend.getAccess().equals("r")) {
                    Config.client.sendMessage("addDocumentById " + friend.getName() + " " + friendNameTextField.getText() + " " + friend.getAccess());
                }
            }
            RichTextDemo richTextWindow = new RichTextDemo();
            richTextWindow.previousView = Config.getPathToViews() + "friends-view.fxml";
            richTextWindow.start((Stage) friendList.getScene().getWindow());
            richTextWindow.documentNameField.setText(friendNameTextField.getText());
        };
        showPopupWindow("Название документа", "Создать", action);
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
