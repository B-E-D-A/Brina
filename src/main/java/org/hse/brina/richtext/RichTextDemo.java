/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.hse.brina.richtext;

import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.*;
import org.hse.brina.Config;
import org.hse.brina.Main;
import org.hse.brina.utils.AudioRecognitionController;
import org.hse.brina.yandexGPT.server.GPTServer;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;

public class RichTextDemo extends Application {

    // the saved/loaded files and their format are arbitrary and may change across versions
    public static final String RTFX_FILE_EXTENSION = ".rtfx";
    private static final Logger logger = LogManager.getLogger();
    public final FoldableStyledArea area = new FoldableStyledArea();
    public final SuspendableNo updatingToolbar = new SuspendableNo();
    private final StringBuilder documentId = new StringBuilder();
    public String previousView = Config.getPathToViews() + "main-window-view.fxml";
    public Stage mainStage;
    private TextField documentNameField = new TextField();
    private Scene mainScene;

    {
        area.setWrapText(true);
        area.setStyleCodecs(ParStyle.CODEC, Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));
        area.setParagraphGraphicFactory(new BulletFactory(area));  // and folded paragraph indicator
        area.setContextMenu(new DefaultContextMenu());
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        Button loadBtn = createButton("loadfile", this::loadDocument, "Load document.\n\n" + "Note: the demo will load only previously-saved \"" + RTFX_FILE_EXTENSION + "\" files. " + "This file format is abitrary and may change across versions.");
        Button saveBtn = createButton("savefile", this::saveDocument, "Save document.\n\n" + "Note: the demo will save the area's content to a \"" + RTFX_FILE_EXTENSION + "\" file. " + "This file format is abitrary and may change across versions.");
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());

        Button backBtn = new Button();
        backBtn.getStyleClass().add("back");
        backBtn.setOnAction(e -> {
            logger.info(documentNameField.getText());
            Config.client.sendMessage("unlockDocument " + documentNameField.getText());
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(previousView));
            Parent pageLoader;
            try {
                pageLoader = loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Scene scene = new Scene(pageLoader, primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());
            primaryStage.setScene(scene);
            area.requestFocus();
        });
        backBtn.setPrefWidth(25);
        backBtn.setPrefHeight(25);
        backBtn.setTooltip(new Tooltip("Back"));
        Button undoBtn = createButton("undo", area::undo, "Undo");
        Button redoBtn = createButton("redo", area::redo, "Redo");
        Button cutBtn = createButton("cut", area::cut, "Cut");
        Button copyBtn = createButton("copy", area::copy, "Copy");
        Button pasteBtn = createButton("paste", area::paste, "Paste");
        Button boldBtn = createButton("bold", this::toggleBold, "Bold");
        Button italicBtn = createButton("italic", this::toggleItalic, "Italic");
        Button underlineBtn = createButton("underline", this::toggleUnderline, "Underline");
        Button strikeBtn = createButton("strikethrough", this::toggleStrikethrough, "Strike Trough");
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        Button increaseIndentBtn = createButton("increaseIndent", this::increaseIndent, "Increase indent");
        Button decreaseIndentBtn = createButton("decreaseIndent", this::decreaseIndent, "Decrease indent");

        ToggleGroup alignmentGrp = new ToggleGroup();
        ToggleButton alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft, "Align left");
        ToggleButton alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter, "Align center");
        ToggleButton alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight, "Align right");
        ToggleButton alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify, "Justify");

        HBox HBoxParagraphBackground = new HBox();
        HBoxParagraphBackground.setStyle("-fx-background-color: white; -fx-alignment: center; -fx-spacing: 5px;");
        HBoxParagraphBackground.setPadding(new Insets(10));
        ColorPicker paragraphBackgroundPicker = new ColorPicker();
        paragraphBackgroundPicker.setStyle(" -fx-color-label-visible: false;");
        Text paragraphBackgroundText = new Text("Paragraph background");
        paragraphBackgroundText.setStyle("-fx-text-alignment: left; -fx-alignment: center-left; -fx-font-size: 12px");
        HBoxParagraphBackground.getChildren().addAll(paragraphBackgroundPicker, paragraphBackgroundText);

        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.setStyle(" -fx-color-label-visible: false;");
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setTooltip(new Tooltip("Font size"));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.setStyle(" -fx-color-label-visible: false;");
        familyCombo.getSelectionModel().select("Serif");
        familyCombo.setTooltip(new Tooltip("Font family"));

        HBox HBoxText = new HBox();
        HBoxText.setStyle("-fx-background-color: white; -fx-alignment: center; -fx-spacing: 5px;");
        HBoxText.setPadding(new Insets(10));
        ColorPicker textColorPicker = new ColorPicker(Color.BLACK);
        textColorPicker.setStyle(" -fx-color-label-visible: false;");
        Text textColorText = new Text("Текст");
        textColorText.setStyle("-fx-text-alignment: left; -fx-alignment: center-left;-fx-font-size: 12px");
        HBoxText.getChildren().addAll(textColorPicker, textColorText);

        HBox HBoxBackground = new HBox();
        HBoxBackground.setStyle("-fx-background-color: white; -fx-alignment: center; -fx-spacing: 5px;");
        HBoxBackground.setPadding(new Insets(10));
        ColorPicker backgroundColorPicker = new ColorPicker();
        backgroundColorPicker.setStyle(" -fx-color-label-visible: false;");
        Text backgroundColorText = new Text("Фон");
        backgroundColorText.setStyle("-fx-text-alignment: left; -fx-alignment: center-left; -fx-font-size: 12px");
        HBoxBackground.getChildren().addAll(backgroundColorPicker, backgroundColorText);

        paragraphBackgroundPicker.setTooltip(new Tooltip("Paragraph background"));
        textColorPicker.setTooltip(new Tooltip("Text color"));
        backgroundColorPicker.setTooltip(new Tooltip("Text background"));

        paragraphBackgroundPicker.valueProperty().addListener((o, old, color) -> updateParagraphBackground(color));
        sizeCombo.setOnAction(evt -> updateFontSize(sizeCombo.getValue()));
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        backgroundColorPicker.valueProperty().addListener((o, old, color) -> updateBackgroundColor(color));

        undoBtn.disableProperty().bind(area.undoAvailableProperty().map(x -> !x));
        redoBtn.disableProperty().bind(area.redoAvailableProperty().map(x -> !x));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            {
                bind(area.selectionProperty());
            }

            @Override
            protected boolean computeValue() {
                return area.getSelection().getLength() == 0;
            }
        };

        cutBtn.disableProperty().bind(selectionEmpty);
        copyBtn.disableProperty().bind(selectionEmpty);

        area.beingUpdatedProperty().addListener((o, old, beingUpdated) -> {
            if (!beingUpdated) {
                boolean bold, italic, underline, strike;
                Integer fontSize;
                String fontFamily;
                Color textColor;
                Color backgroundColor;

                IndexRange selection = area.getSelection();
                if (selection.getLength() != 0) {
                    StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
                    bold = styles.styleStream().anyMatch(s -> s.bold.orElse(false));
                    italic = styles.styleStream().anyMatch(s -> s.italic.orElse(false));
                    underline = styles.styleStream().anyMatch(s -> s.underline.orElse(false));
                    strike = styles.styleStream().anyMatch(s -> s.strikethrough.orElse(false));
                    int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                    fontSize = sizes.length == 1 ? sizes[0] : -1;
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                    textColor = colors.length == 1 ? colors[0] : null;
                    Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                    backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
                } else {
                    int p = area.getCurrentParagraph();
                    int col = area.getCaretColumn();
                    TextStyle style = area.getStyleAtPosition(p, col);
                    bold = style.bold.orElse(false);
                    italic = style.italic.orElse(false);
                    underline = style.underline.orElse(false);
                    strike = style.strikethrough.orElse(false);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                int startPar = area.offsetToPosition(selection.getStart(), Forward).getMajor();
                int endPar = area.offsetToPosition(selection.getEnd(), Backward).getMajor();
                List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = area.getParagraphs().subList(startPar, endPar + 1);

                @SuppressWarnings("unchecked") Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct().toArray(Optional[]::new);
                Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

                @SuppressWarnings("unchecked") Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor).distinct().toArray(Optional[]::new);
                Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0] : Optional.empty();

                updatingToolbar.suspendWhile(() -> {
                    if (bold) {
                        if (!boldBtn.getStyleClass().contains("pressed")) {
                            boldBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        boldBtn.getStyleClass().remove("pressed");
                    }

                    if (italic) {
                        if (!italicBtn.getStyleClass().contains("pressed")) {
                            italicBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        italicBtn.getStyleClass().remove("pressed");
                    }

                    if (underline) {
                        if (!underlineBtn.getStyleClass().contains("pressed")) {
                            underlineBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        underlineBtn.getStyleClass().remove("pressed");
                    }

                    if (strike) {
                        if (!strikeBtn.getStyleClass().contains("pressed")) {
                            strikeBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        strikeBtn.getStyleClass().remove("pressed");
                    }

                    if (alignment.isPresent()) {
                        TextAlignment al = alignment.get();
                        switch (al) {
                            case LEFT:
                                alignmentGrp.selectToggle(alignLeftBtn);
                                break;
                            case CENTER:
                                alignmentGrp.selectToggle(alignCenterBtn);
                                break;
                            case RIGHT:
                                alignmentGrp.selectToggle(alignRightBtn);
                                break;
                            case JUSTIFY:
                                alignmentGrp.selectToggle(alignJustifyBtn);
                                break;
                        }
                    } else {
                        alignmentGrp.selectToggle(null);
                    }

                    paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

                    if (fontSize != -1) {
                        sizeCombo.getSelectionModel().select(fontSize);
                    } else {
                        sizeCombo.getSelectionModel().clearSelection();
                    }

                    if (fontFamily != null) {
                        familyCombo.getSelectionModel().select(fontFamily);
                    } else {
                        familyCombo.getSelectionModel().clearSelection();
                    }

                    if (textColor != null) {
                        textColorPicker.setValue(textColor);
                    }

                    backgroundColorPicker.setValue(backgroundColor);
                });
            }
        });

        TextField documentName = new TextField();
        documentName.setText("Новый документ");
        documentNameField = documentName;
        documentName.setStyle("-fx-font-size: 12px;");
        documentName.setMaxWidth(200);
        documentName.setMaxHeight(50);
        documentName.setPrefHeight(30);
        documentName.setPrefWidth(150);
        documentName.setAlignment(Pos.BASELINE_LEFT);
        ToolBar toolBar1 = new ToolBar(loadBtn, saveBtn, new Separator(Orientation.VERTICAL), undoBtn, redoBtn, new Separator(Orientation.VERTICAL), cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL), boldBtn, italicBtn, underlineBtn, strikeBtn, new Separator(Orientation.VERTICAL), alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, new Separator(Orientation.VERTICAL), increaseIndentBtn, decreaseIndentBtn, new Separator(Orientation.VERTICAL), insertImageBtn, new Separator(Orientation.VERTICAL), HBoxParagraphBackground);

        String toolBarStyle = "-fx-background-color:white; -fx-padding: 0px;";
        toolBar1.setStyle(toolBarStyle);
        toolBar1.setPrefHeight(30);
        toolBar1.setMaxHeight(30);

        HBox documentNameHBox = new HBox();
        Button shareButton = new Button();
        shareButton.setText("Поделиться");
        shareButton.setOnAction(e -> showPopupWindow(primaryStage, documentName));
        shareButton.setAlignment(Pos.TOP_RIGHT);
        shareButton.setStyle("-fx-background-color: #97bd89ff; -fx-text-fill: white; -fx-font-size: 13px; fx-border-color: rgba(67, 76, 85, 0.5); -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-margin-right: 10px; -fx-font-family: \"Montserrat\";");
        documentNameHBox.setStyle("-fx-spacing: 10");

        HBox featuresHBox = new HBox();
        Button recordingButton = new Button();
        recordingButton.getStyleClass().add("microphone");
        recordingButton.setPrefWidth(30);
        recordingButton.setPrefHeight(30);
        recordingButton.setOnAction(e -> {
            Stage popupVoiceStage = new Stage();
            popupVoiceStage.initOwner(primaryStage);
            popupVoiceStage.initModality(Modality.NONE);
            AudioRecognitionController controller = new AudioRecognitionController();
            controller.documentArea = area;
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(Config.getPathToViews() + "audio-recognition-view.fxml"));
            loader.setControllerFactory(param -> controller);
            Parent root;
            try {
                root = loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Scene scene = new Scene(root, mainStage.getScene().getWidth(), mainStage.getScene().getHeight());
            popupVoiceStage.setScene(scene);
            try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
                Image icon = new Image(iconStream);
                popupVoiceStage.getIcons().add(icon);
            } catch (Exception error) {
                logger.error(error.getMessage());
            }
            popupVoiceStage.showAndWait();
        });

        ContextMenu contextMenu = area.getContextMenu();
        contextMenu.getItems().clear();
        MenuItem firstOption = new MenuItem("Rewrite text in a formal style");
        firstOption.setOnAction(e -> {
            showResponse("Rewrite text in a formal style");
        });
        MenuItem secondOption = new MenuItem("Correct grammatical errors and spelling mistakes");
        secondOption.setOnAction(e -> {
            showResponse("Иправь все ошибки и опечатки и пришли исправленный текст");
        });
        MenuItem thirdOption = new MenuItem("Make the text more concise");
        thirdOption.setOnAction(e -> {
            showResponse("Make the text more concise");
        });
        MenuItem fourthOption = new MenuItem("Translate text into...");
        contextMenu.getItems().addAll(firstOption, secondOption, thirdOption);
        area.setOnContextMenuRequested(e -> {
            contextMenu.show(area, e.getScreenX(), e.getScreenY());
        });

        Button chatGPTButton = new Button();
        chatGPTButton.getStyleClass().add("yandex-gpt");
        chatGPTButton.setPrefWidth(30);
        chatGPTButton.setPrefHeight(30);
        chatGPTButton.setOnAction(e -> {
            Stage popupChatStage = new Stage();
            popupChatStage.initOwner(primaryStage);
            popupChatStage.initModality(Modality.NONE);
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(Config.getPathToViews() + "yandex-gpt-view.fxml"));
            Parent root;
            try {
                root = loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Scene scene = new Scene(root, mainStage.getScene().getWidth(), mainStage.getScene().getHeight());
            popupChatStage.setScene(scene);
            try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
                Image icon = new Image(iconStream);
                popupChatStage.getIcons().add(icon);
            } catch (Exception error) {
                logger.error(error.getMessage());
            }
            popupChatStage.showAndWait();
        });

        featuresHBox.setStyle("-fx-background-color: white; -fx-alignment: TOP_RIGHT; -fx-spacing: 1;");
        featuresHBox.setAlignment(Pos.TOP_RIGHT);
        featuresHBox.getChildren().addAll(recordingButton, chatGPTButton);
        featuresHBox.setPrefHeight(30);
        featuresHBox.setPrefWidth(280);
        featuresHBox.setMinWidth(70);
        featuresHBox.setMaxWidth(500);
        HBox.setHgrow(featuresHBox, Priority.ALWAYS);

        documentNameHBox.getChildren().addAll(backBtn, documentName, featuresHBox, shareButton);

        VBox upperToolbarVBox = new VBox();
        upperToolbarVBox.setStyle("-fx-background-color: white; -fx-alignment: TOP_LEFT; -fx-spacing: 1px;");
        upperToolbarVBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(upperToolbarVBox, Priority.ALWAYS);
        VBox.setVgrow(upperToolbarVBox, Priority.ALWAYS);
        upperToolbarVBox.getChildren().addAll(documentNameHBox, toolBar1);

        HBox upperToolbarHBox = new HBox();
        upperToolbarHBox.setStyle("-fx-background-color: white; -fx-alignment: TOP_LEFT; -fx-spacing: 0px;");
        upperToolbarHBox.setAlignment(Pos.TOP_LEFT);
        upperToolbarHBox.setPadding(new Insets(3));
        HBox.setHgrow(upperToolbarHBox, Priority.ALWAYS);
        upperToolbarHBox.getChildren().addAll(upperToolbarVBox);

        ToolBar toolBar2 = new ToolBar(sizeCombo, familyCombo, HBoxText, HBoxBackground);
        toolBar2.setStyle(toolBarStyle);
        toolBar2.setPrefHeight(30);
        toolBar2.setMaxHeight(30);

        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox windowVBox = new VBox();
        windowVBox.setStyle("-fx-background-color: white; -fx-alignment: TOP_LEFT; -fx-spacing: 1px;");
        windowVBox.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(vsPane, Priority.ALWAYS);

        windowVBox.getChildren().addAll(upperToolbarHBox, new Separator(Orientation.HORIZONTAL), toolBar2, new Separator(Orientation.HORIZONTAL), vsPane);

        Scene scene = new Scene(windowVBox, primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());

        if (RichTextDemo.class.getResource("rich-text.css") != null) {
            scene.getStylesheets().add(RichTextDemo.class.getResource("rich-text.css").toExternalForm());
        } else {
            logger.info("rich-text.css not found!");
        }

        primaryStage.setScene(scene);
        mainScene = scene;
        area.requestFocus();
        primaryStage.show();
    }

    private void showResponse(String option) {
        String text = area.getSelectedText();
        try {
            String textGPT = GPTServer.getGPTProcessing(option, text);
            Stage GPTPopupStage = new Stage();
            GPTPopupStage.initOwner(mainStage);
            GPTPopupStage.initModality(Modality.NONE);
            Text GPTResultText = new Text("YandexGPT");
            GPTResultText.getStyleClass().add("simple-text");
            GPTResultText.setFill(Color.BLACK);
            TextArea GPTArea = new TextArea();
            GPTArea.setWrapText(true);
            GPTArea.setText(textGPT);
            GPTArea.setStyle("-fx-background-color: white; -fx-border-color: rgba(67, 76, 85, 0.5); -fx-border-width: 1px; -fx-border-radius: 8px");
            GPTArea.setMaxWidth(440);
            GPTArea.setMaxHeight(290);
            GPTArea.setPrefHeight(290);
            GPTArea.setPrefWidth(440);
            VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            vBox.setSpacing(10);
            vBox.setMaxWidth(300);
            Button pasteButton = new Button();
            pasteButton.setText("Вставить ответ");
            pasteButton.getStyleClass().add("dark-button");
            pasteButton.setStyle("-fx-background-color: #97bd89ff; -fx-border-color: rgba(67, 76, 85, 0.5)");
            pasteButton.setOnAction(event -> {
                area.replaceSelection(GPTArea.getText());
                GPTPopupStage.close();
            });
            vBox.setStyle("-fx-background-color: white; -fx-padding: 10");
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().addAll(GPTResultText, GPTArea, pasteButton);

            int stageHeight = 300;
            int stageWidth = 450;
            Scene popupScene = new Scene(vBox, stageWidth, stageHeight);
            popupScene.getStylesheets().add(Config.getPathToCss() + "sign-in-page-style.css");
            GPTPopupStage.setScene(popupScene);
            if (mainScene != null) {
                Window previousWindow = mainScene.getWindow();
                double centerX = previousWindow.getX() + (previousWindow.getWidth() - stageWidth) / 2;
                double centerY = previousWindow.getY() + (previousWindow.getHeight() - stageHeight) / 2;
                GPTPopupStage.setX(centerX);
                GPTPopupStage.setY(centerY);
            }
            try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
                Image icon = new Image(iconStream);
                GPTPopupStage.getIcons().add(icon);
            } catch (Exception exe) {
                logger.error(exe.getMessage());
            }

            GPTPopupStage.setResizable(false);
            GPTPopupStage.showAndWait();

        } catch (URISyntaxException ex) {
            logger.info("Error while constructing request");
        } catch (IOException ex) {
            logger.info("Error while sending request");
        } catch (InterruptedException ex) {
            logger.info(ex.getMessage());
        }
    }

    private void showPopupWindow(Stage primaryStage, TextField documentName) {
        Stage popupStage = new Stage();
        popupStage.initOwner(primaryStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        Text documentNameText = new Text("Название:");
        documentNameText.setStyle("-fx-font-size: 14px; -fx-font-family: \"Montserrat\"; -fx-text-fill: #434c55ff; -fx-background-color: white");
        documentNameText.setFill(Color.BLACK);
        TextField documentNameTextField = new TextField();
        documentNameTextField.setText(documentName.getText());
        documentNameTextField.setStyle("-fx-font-size: 14px");
        documentNameTextField.getStyleClass().add("login-field");
        documentNameTextField.setMaxWidth(205);
        documentNameTextField.setMaxHeight(30);
        documentNameTextField.setPrefHeight(30);
        documentNameTextField.setPrefWidth(205);
        documentNameTextField.setAlignment(Pos.BASELINE_LEFT);
        HBox nameHBox = new HBox();
        nameHBox.getChildren().addAll(documentNameText, documentNameTextField);
        nameHBox.setAlignment(Pos.CENTER);
        nameHBox.setSpacing(10);
        nameHBox.setMaxWidth(265);

        TextField usersTextField = new TextField();
        usersTextField.setPromptText("Пользователь с доступом к документу");
        usersTextField.setPrefWidth(265);
        usersTextField.setPrefHeight(30);
        usersTextField.setMaxWidth(265);
        usersTextField.setMaxHeight(30);
        usersTextField.getStyleClass().add("login-field");
        usersTextField.setStyle("-fx-font-size: 12px; -fx-font-family: \"Montserrat\"; -fx-text-fill: #434c55ff; -fx-background-color: white");
        usersTextField.setAlignment(Pos.CENTER);

        Text accessText = new Text("Права доступа:");
        accessText.setStyle("-fx-font-size: 14px; -fx-font-family: \"Montserrat\"; -fx-text-fill: #434c55ff");
        accessText.setFill(Color.BLACK);
        accessText.setTextAlignment(TextAlignment.CENTER);
        Button readerButton = new Button();
        readerButton.setText("Читатель");
        readerButton.getStyleClass().add("access-button");
        readerButton.setStyle("-fx-background-color: white");
        Button editorButton = new Button();
        editorButton.setText("Редактор");
        editorButton.getStyleClass().add("access-button");
        editorButton.setStyle("-fx-background-color: white");
        StringBuilder rights = new StringBuilder();
        readerButton.setOnAction(event -> {
            editorButton.setStyle("-fx-background-color: white; -fx-text-fill: #5479B8; -fx-border-color: #5479B8;");
            rights.replace(0, rights.length(), "r");
            readerButton.setStyle("-fx-background-color: #97bd89ff; -fx-text-fill: #434c55ff; -fx-border-color: #434c55ff;");
        });
        editorButton.setOnAction(event -> {
            readerButton.setStyle("-fx-background-color: white; -fx-text-fill: #5479B8; -fx-border-color: #5479B8;");
            rights.replace(0, rights.length(), "w");
            editorButton.setStyle("-fx-background-color: #97bd89ff; -fx-text-fill: #434c55ff; -fx-border-color: #434c55ff;");
        });
        HBox rightsHBox = new HBox();
        rightsHBox.setSpacing(10);
        rightsHBox.setAlignment(Pos.CENTER);
        rightsHBox.getChildren().addAll(readerButton, editorButton);
        rightsHBox.getStyleClass().add("white-box");
        rightsHBox.setStyle("-fx-background-color: white");

        VBox shareVBox = new VBox();
        shareVBox.setPrefWidth(265);
        shareVBox.setPrefHeight(30);
        shareVBox.setMaxWidth(265);
        shareVBox.setMaxHeight(30);
        shareVBox.setAlignment(Pos.CENTER);
        shareVBox.setSpacing(10);
        shareVBox.setPadding(new Insets(0, 0, 10, 0));

        VBox buttonsVBox = new VBox();
        buttonsVBox.setAlignment(Pos.CENTER);
        Button keyButton = new Button();
        keyButton.setText("Открыть доступ");
        keyButton.getStyleClass().add("dark-button");
        keyButton.setPrefWidth(265);
        keyButton.setAlignment(Pos.CENTER);
        TextArea IDArea = new TextArea();
        VBox popupLayout = new VBox(nameHBox, usersTextField, accessText, buttonsVBox, keyButton);
        popupLayout.setAlignment(Pos.CENTER);
        popupLayout.getStyleClass().add("white-box");
        popupLayout.setStyle("-fx-background-color: white");
        popupLayout.setSpacing(10);
        keyButton.setOnAction(event -> {
            shareVBox.getChildren().clear();
            if (!usersTextField.getText().isEmpty()) {
                Stage IDStage = new Stage();
                IDStage.initOwner(popupStage);
                IDStage.initModality(Modality.APPLICATION_MODAL);

                int ID = documentNameTextField.getText().hashCode();
                documentId.replace(0, documentId.length(), Integer.toString(ID));
                Text shareText = new Text("У " + usersTextField.getText() + " теперь есть доступ по ключу:");
                shareText.setStyle("-fx-font-size: 14px; -fx-font-family: \"Montserrat\"; -fx-text-fill: #434c55ff");
                shareText.setFill(Color.BLACK);
                shareText.setTextAlignment(TextAlignment.CENTER);
                IDArea.setText(Integer.toString(Math.abs(ID)));
                IDArea.setPrefColumnCount(1);
                IDArea.setPrefRowCount(1);
                IDArea.setPrefWidth(265);
                IDArea.setMaxWidth(265);
                IDArea.setPrefHeight(20);
                IDArea.setMaxHeight(20);
                IDArea.setEditable(false);
                IDArea.setStyle("-fx-font-size: 14px; -fx-font-family: \"Montserrat\"; -fx-text-fill: #434c55ff; -fx-background-color: white");
                IDArea.getStyleClass().add("text-area");
                shareVBox.getChildren().addAll(shareText, IDArea);

                VBox globalVBox = new VBox(shareVBox);
                globalVBox.setAlignment(Pos.CENTER);
                globalVBox.setStyle("-fx-background-color: white");
                globalVBox.setSpacing(10);
                int smallStageHeight = 85;
                int smallStageWidth = 310;
                Scene smallScene = new Scene(globalVBox, smallStageWidth, smallStageHeight);
                smallScene.getStylesheets().add(Config.getPathToCss() + "sign-in-page-style.css");
                IDStage.setScene(smallScene);
                if (mainScene != null) {
                    Window previousWindow = mainScene.getWindow();
                    double centerX = previousWindow.getX() + (previousWindow.getWidth() - smallStageWidth) / 2;
                    double centerY = previousWindow.getY() + (previousWindow.getHeight() - smallStageHeight) / 2;
                    IDStage.setX(centerX);
                    IDStage.setY(centerY);
                }
                try (InputStream iconStream = getClass().getResourceAsStream(Config.getPathToAssets() + "icon.png")) {
                    Image icon = new Image(iconStream);
                    IDStage.getIcons().add(icon);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                IDStage.setResizable(false);
                IDStage.showAndWait();
                Config.client.sendMessage("addDocumentById " + usersTextField.getText() + " " + documentNameTextField.getText() + " " + rights);
            }
        });

        buttonsVBox.getChildren().addAll(rightsHBox, keyButton);
        buttonsVBox.setSpacing(10);
        buttonsVBox.setStyle("-fx-background-color: white");
        int stageHeight = 250;
        int stageWidth = 310;
        Scene popupScene = new Scene(popupLayout, stageWidth, stageHeight);
        popupScene.getStylesheets().add(Config.getPathToCss() + "sign-in-page-style.css");
        popupStage.setScene(popupScene);
        if (mainScene != null) {
            Window previousWindow = mainScene.getWindow();
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
        popupStage.setOnHidden(event -> {
            if (!documentNameTextField.getText().contentEquals(documentName.getText())) {
                documentName.setText(documentNameTextField.getText());
            }
        });
        popupStage.setResizable(false);
        popupStage.showAndWait();
    }

    public Button createButton(String styleClass, Runnable action, String toolTip) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    public ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action, String toolTip) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(grp);
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    public void toggleBold() {
        updateStyleInSelection(spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    public void toggleItalic() {
        updateStyleInSelection(spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    public void toggleUnderline() {
        updateStyleInSelection(spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    public void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle.strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
    }

    public void alignLeft() {
        updateParagraphStyleInSelection(ParStyle.alignLeft());
    }

    public void alignCenter() {
        updateParagraphStyleInSelection(ParStyle.alignCenter());
    }

    public void alignRight() {
        updateParagraphStyleInSelection(ParStyle.alignRight());
    }

    public void alignJustify() {
        updateParagraphStyleInSelection(ParStyle.alignJustify());
    }

    public void loadDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load document");
        fileChooser.setInitialDirectory(new File(Config.getProjectPath()));

        setExtensions(fileChooser);
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            area.clear();
            String selectedExtension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(2);
            switch (selectedExtension) {
                case "rtfx" -> loadRTFX(selectedFile);
                case "txt" -> loadTXT(selectedFile);
            }
        }
    }

    private void setExtensions(FileChooser fileChooser) {
        FileChooser.ExtensionFilter rtfxExtension = new FileChooser.ExtensionFilter("RTFX Files (*.rtfx)", "*.rtfx");
        FileChooser.ExtensionFilter txtExtension = new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
        FileChooser.ExtensionFilter pdfExtension = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf");
        FileChooser.ExtensionFilter docxExtension = new FileChooser.ExtensionFilter("Word Documents (*.docx)", "*.docx");
        fileChooser.getExtensionFilters().addAll(rtfxExtension, txtExtension, pdfExtension, docxExtension);
    }

    private void loadTXT(File file) {
        logger.info("Loading TXT File");
        try (FileReader reader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            area.replaceText(String.valueOf(content));
            String name = file.toPath().getFileName().toString().replace(".rtfx", "");
            documentNameField.setText(name);
        } catch (IOException e) {
            logger.error("Error while loading TXT file: " + e.getMessage());
        }
    }

    public void loadRTFX(File file) {
        if (area.getStyleCodecs().isPresent()) {
            Tuple2<Codec<ParStyle>, Codec<StyledSegment<Either<String, LinkedImage>, TextStyle>>> codecs = area.getStyleCodecs().get();
            Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());

            try {
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis);
                StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = codec.decode(dis);
                fis.close();

                if (doc != null) {
                    area.replaceSelection(doc);
                }
                String name = file.toPath().getFileName().toString().replace(".rtfx", "");
                documentNameField.setText(name);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void saveDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save document");
        fileChooser.setInitialDirectory(new File(Config.getProjectPath()));
        fileChooser.setInitialFileName(documentNameField.getText() + RTFX_FILE_EXTENSION);

        setExtensions(fileChooser);

        File selectedFile = fileChooser.showSaveDialog(mainStage);
        if (selectedFile != null) {
            Path filePath = selectedFile.toPath();
            String fileName = filePath.getFileName().toString();
            String fileExtension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(2);
            Path newPath = Paths.get(Config.getProjectPath().substring(0, Config.getProjectPath().length() - Config.getAppNameLength()) + "documents/" + fileName.replace(".rtfx", "." + fileExtension));
            switch (fileExtension) {
                case "rtfx" -> saveInRTFXFormat(selectedFile);
                case "txt" -> saveInTXTFormat(selectedFile);
            }
            String name = fileName.replace("." + fileExtension, "");
            documentNameField.setText(name);
            try {
                Files.copy(filePath, newPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error while saving to Documents folder " + e.getMessage());
            }
            Config.client.sendMessage("saveDocument " + fileName + " " + Config.client.getName() + " " + name);
            Config.client.sendMessage("unlockDocument " + documentNameField.getText());
        }
    }

    private void saveInTXTFormat(File file) {
        StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = area.getDocument();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(doc.getText());
            writer.flush();
        } catch (IOException e) {
            logger.error("Error while saving the file in TXT format: " + e.getMessage());
        }

    }


    public void saveInRTFXFormat(File file) {
        StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = area.getDocument();
        area.getStyleCodecs().ifPresent(codecs -> {
            Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
            try {
                FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos);
                codec.encode(dos, doc);
                fos.close();
            } catch (IOException e) {
                logger.error("Error while saving the file in RTFX format" + e.getMessage());
            }
        });
    }

    /**
     * Action listener which inserts a new image at the current caret position.
     */
    public void insertImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert image");
        fileChooser.setInitialDirectory(new File(Config.getProjectPath()));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            imagePath = imagePath.replace('\\', '/');
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros = ReadOnlyStyledDocument.fromSegment(Either.right(new RealLinkedImage(imagePath)), ParStyle.EMPTY, TextStyle.EMPTY, area.getSegOps());
            area.replaceSelection(ros);
        }
    }

    public void increaseIndent() {
        updateParagraphStyleInSelection(ps -> ps.increaseIndent());
    }

    public void decreaseIndent() {
        updateParagraphStyleInSelection(ps -> ps.decreaseIndent());
    }

    public void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    public void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    public void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = area.getSelection();
        int startPar = area.offsetToPosition(selection.getStart(), Forward).getMajor();
        int endPar = area.offsetToPosition(selection.getEnd(), Backward).getMajor();
        for (int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = area.getParagraph(i);
            area.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
    }

    public void updateParagraphStyleInSelection(ParStyle mixin) {
        updateParagraphStyleInSelection(style -> style.updateWith(mixin));
    }

    public void updateFontSize(Integer size) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontSize(size));
        }
    }

    public void updateFontFamily(String family) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontFamily(family));
        }
    }

    public void updateTextColor(Color color) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.textColor(color));
        }
    }

    public void updateBackgroundColor(Color color) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.backgroundColor(color));
        }
    }

    public void updateParagraphBackground(Color color) {
        if (!updatingToolbar.get()) {
            updateParagraphStyleInSelection(ParStyle.backgroundColor(color));
        }
    }

    public static class FoldableStyledArea extends GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> {
        public final static TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
        public final static LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();

        public FoldableStyledArea() {
            super(ParStyle.EMPTY,                                                 // default paragraph style
                    (paragraph, style) -> paragraph.setStyle(style.toCss()),        // paragraph style setter
                    TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),  // default segment style
                    styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()),                            // segment operations
                    seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter
        }

        public static Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg, BiConsumer<? super TextExt, TextStyle> applyStyle) {
            return seg.getSegment().unify(text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle), LinkedImage::createNode);
        }

        public void foldParagraphs(int startPar, int endPar) {
            foldParagraphs(startPar, endPar, getAddFoldStyle());
        }

        public void foldSelectedParagraphs() {
            foldSelectedParagraphs(getAddFoldStyle());
        }

        public void foldText(int start, int end) {
            fold(start, end, getAddFoldStyle());
        }

        public void unfoldParagraphs(int startingFromPar) {
            unfoldParagraphs(startingFromPar, getFoldStyleCheck(), getRemoveFoldStyle());
        }

        public void unfoldText(int startingFromPos) {
            startingFromPos = offsetToPosition(startingFromPos, Bias.Backward).getMajor();
            unfoldParagraphs(startingFromPos, getFoldStyleCheck(), getRemoveFoldStyle());
        }

        protected UnaryOperator<ParStyle> getAddFoldStyle() {
            return pstyle -> pstyle.updateFold(true);
        }

        protected UnaryOperator<ParStyle> getRemoveFoldStyle() {
            return pstyle -> pstyle.updateFold(false);
        }

        protected Predicate<ParStyle> getFoldStyleCheck() {
            return pstyle -> pstyle.isFolded();
        }
    }

    public class DefaultContextMenu extends ContextMenu {
        public MenuItem fold, unfold;

        public DefaultContextMenu() {
            fold = new MenuItem("Fold selected text");
            fold.setOnAction(AE -> {
                hide();
                fold();
            });

            unfold = new MenuItem("Unfold from cursor");
            unfold.setOnAction(AE -> {
                hide();
                unfold();
            });

            getItems().addAll(fold, unfold);
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        public void fold() {
            ((FoldableStyledArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        public void unfold() {
            FoldableStyledArea area = (FoldableStyledArea) getOwnerNode();
            area.unfoldParagraphs(area.getCurrentParagraph());

        }
    }
}
