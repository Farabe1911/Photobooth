package com.example.photobooth;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PhotoBooth extends Application {

    private static final int CAPTURE_COUNT = 5;

    private Stage primaryStage;
    private Scene setupScene;
    private Scene captureScene;

    private ComboBox<Webcam> webcamBox;
    private TextField eventField;
    private ColorPicker colorPicker;
    private ComboBox<Integer> fontSizeBox;
    private ComboBox<Printer> printerBox;
    private ComboBox<TextStyle> textStyleBox;
    private ToggleGroup frameToggleGroup;
    private Label frameStatementPreview;

    private Webcam webcam;
    private ImageView videoView;
    private Label captureInstruction;
    private Label captureTimerOverlay;
    private Label captureHeaderLabel;
    private Button startCycleButton;
    private StackPane videoStack;
    private Rectangle videoClip;
    private final ImageView[] captureThumbnails = new ImageView[CAPTURE_COUNT];
    private final StackPane[] captureThumbnailFrames = new StackPane[CAPTURE_COUNT];
    private final Label[] captureThumbnailBadges = new Label[CAPTURE_COUNT];

    private boolean captureInProgress;
    private final List<BufferedImage> capturedImages = new ArrayList<>();
    private FrameTheme selectedTheme = FrameTheme.BIRTHDAY_CELEBRATION;
    private TextStyle selectedTextStyle = TextStyle.SCRIPT;
    private Printer selectedPrinter;

    private File saveDirectory = new File("photos");
    private Label saveLocationLabel;
    private String currentSessionStamp;

    private String stylesheet;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("PhotoBooth Studio");

        setupScene = buildSetupScene();
        captureScene = buildCaptureScene();

        primaryStage.setMinWidth(1040);
        primaryStage.setMinHeight(760);
        primaryStage.setScene(setupScene);
        primaryStage.show();
    }

    private Scene buildSetupScene() {
        webcamBox = new ComboBox<>();
        webcamBox.setPrefWidth(260);
        loadAvailableWebcams();

        Button refreshCameras = new Button("Refresh Cameras");
        refreshCameras.getStyleClass().add("secondary-button");
        refreshCameras.setOnAction(e -> loadAvailableWebcams());

        HBox cameraRow = new HBox(12, webcamBox, refreshCameras);
        cameraRow.setAlignment(Pos.CENTER_LEFT);

        eventField = new TextField("my event");
        eventField.setPromptText("Event name or honoree");
        eventField.textProperty().addListener((obs, old, val) -> updateFrameStatementPreview());

        fontSizeBox = new ComboBox<>();
        fontSizeBox.getItems().addAll(24, 30, 36, 48, 60, 72);
        fontSizeBox.getSelectionModel().select(Integer.valueOf(36));

        colorPicker = new ColorPicker(Color.web("#f1f5f9"));

        FlowPane controlFlow = new FlowPane();
        controlFlow.getStyleClass().add("control-flow");
        controlFlow.setHgap(18);
        controlFlow.setVgap(16);
        controlFlow.setPrefWrapLength(840);

        textStyleBox = new ComboBox<>();
        textStyleBox.getItems().addAll(TextStyle.values());
        textStyleBox.getSelectionModel().select(TextStyle.SCRIPT);

        printerBox = new ComboBox<>();
        printerBox.setPrefWidth(260);
        printerBox.setPromptText("Use system default");
        printerBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Printer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "System Default Printer" : item.getName());
            }
        });
        printerBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Printer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "System Default Printer" : item.getName());
            }
        });
        printerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Printer printer) {
                return printer == null ? "System Default Printer" : printer.getName();
            }

            @Override
            public Printer fromString(String string) {
                return printerBox.getItems().stream()
                        .filter(p -> p != null && p.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        Button refreshPrinters = new Button("Refresh Printers");
        refreshPrinters.getStyleClass().add("secondary-button");
        refreshPrinters.setOnAction(e -> loadAvailablePrinters());
        HBox printerRow = new HBox(12, printerBox, refreshPrinters);
        printerRow.setAlignment(Pos.CENTER_LEFT);
        loadAvailablePrinters();

        controlFlow.getChildren().addAll(
                buildField("Camera", cameraRow),
                buildField("Event Name", eventField),
                buildField("Font Size", fontSizeBox),
                buildField("Accent Color", colorPicker),
                buildField("Event Text Style", textStyleBox),
                buildField("Printer", printerRow));

        frameToggleGroup = new ToggleGroup();
        FlowPane frameSelector = new FlowPane();
        frameSelector.getStyleClass().add("frame-selector");
        frameSelector.setHgap(18);
        frameSelector.setVgap(18);

        for (FrameTheme theme : FrameTheme.values()) {
            ToggleButton toggle = createFrameToggle(theme);
            frameSelector.getChildren().add(toggle);
            if (theme == selectedTheme) {
                toggle.setSelected(true);
                frameToggleGroup.selectToggle(toggle);
            }
        }

        frameToggleGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle != null && toggle.getUserData() instanceof FrameTheme theme) {
                selectedTheme = theme;
                updateFrameStatementPreview();
            }
        });

        frameStatementPreview = new Label();
        frameStatementPreview.getStyleClass().add("frame-preview-text");
        frameStatementPreview.setWrapText(true);
        frameStatementPreview.setAlignment(Pos.CENTER_LEFT);

        Label previewTitle = new Label("Preview statement");
        previewTitle.getStyleClass().add("field-label");
        VBox framePreviewCard = new VBox(10, previewTitle, frameStatementPreview);
        framePreviewCard.getStyleClass().add("frame-preview-card");

        Button chooseSaveFolder = new Button("Save Folder");
        chooseSaveFolder.getStyleClass().add("secondary-button");
        chooseSaveFolder.setOnAction(e -> chooseSaveDirectory());

        saveLocationLabel = new Label();
        saveLocationLabel.getStyleClass().add("save-location-label");
        updateSaveLocationLabel();

        Button startSession = new Button("Begin Photo Session");
        startSession.getStyleClass().add("primary-button");
        startSession.setOnAction(e -> beginSession());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionRow = new HBox(14, chooseSaveFolder, saveLocationLabel, spacer, startSession);
        actionRow.getStyleClass().add("action-bar");
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Label subtitle = new Label("Capture vibrant photo strips with templated frames.");
        subtitle.getStyleClass().add("app-subtitle");

        Label title = new Label("PhotoBooth Studio");
        title.getStyleClass().add("app-title");

        VBox header = new VBox(6, title, subtitle);
        header.getStyleClass().add("app-header");

        VBox configBody = new VBox(18, controlFlow, frameSelector, framePreviewCard);
        configBody.getStyleClass().add("setup-body");
        configBody.setFillWidth(true);
        configBody.setMinHeight(Region.USE_PREF_SIZE);

        ScrollPane bodyScroller = new ScrollPane(configBody);
        bodyScroller.setFitToWidth(true);
        bodyScroller.setFitToHeight(false);
        bodyScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroller.getStyleClass().add("setup-scroll");
        bodyScroller.setPannable(true);

        BorderPane controlCard = new BorderPane();
        controlCard.getStyleClass().add("control-card");
        controlCard.setTop(header);
        controlCard.setCenter(bodyScroller);
        controlCard.setBottom(actionRow);
        BorderPane.setMargin(header, new Insets(0, 0, 18, 0));
        BorderPane.setMargin(bodyScroller, new Insets(0));
        BorderPane.setMargin(actionRow, new Insets(24, 0, 0, 0));
        controlCard.setMaxWidth(880);
        controlCard.setPrefWidth(880);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        StackPane centered = new StackPane(controlCard);
        centered.setAlignment(Pos.TOP_CENTER);
        root.setCenter(centered);
        BorderPane.setMargin(centered, new Insets(0, 0, 24, 0));

        Scene scene = new Scene(root, 1040, 720);
        applyStyles(scene);
        updateFrameStatementPreview();
        return scene;
    }

    private Scene buildCaptureScene() {
        videoView = new ImageView();
        videoView.setPreserveRatio(true);
        videoView.setSmooth(true);
        videoView.getStyleClass().add("video-view");

        captureInstruction = new Label("Your live preview will appear here shortly.");
        captureInstruction.getStyleClass().add("capture-instruction");
        captureInstruction.setWrapText(true);
        captureInstruction.setAlignment(Pos.TOP_CENTER);
        captureInstruction.setMaxWidth(520);

        captureTimerOverlay = new Label();
        captureTimerOverlay.getStyleClass().add("capture-timer");
        captureTimerOverlay.setVisible(false);

        startCycleButton = new Button("Begin Session");
        startCycleButton.getStyleClass().addAll("primary-button", "overlay-start-button");
        startCycleButton.setDisable(true);
        startCycleButton.setVisible(false);
        startCycleButton.setManaged(false);
        startCycleButton.setOnAction(e -> {
            if (!captureInProgress) {
                startCaptureSequence();
            }
        });

        VBox thumbnailList = new VBox(12);
        thumbnailList.getStyleClass().add("capture-thumb-list");
        thumbnailList.setFillWidth(true);
        for (int i = 0; i < captureThumbnails.length; i++) {
            StackPane slot = createThumbnailSlot(i);
            captureThumbnailFrames[i] = slot;
            thumbnailList.getChildren().add(slot);
        }

        Label thumbHeader = new Label("Captured Preview");
        thumbHeader.getStyleClass().add("capture-thumb-title");
        thumbHeader.setWrapText(true);

        VBox thumbnailColumn = new VBox(18, thumbHeader, thumbnailList);
        thumbnailColumn.getStyleClass().add("capture-thumb-column");
        thumbnailColumn.setPrefWidth(300);
        thumbnailColumn.setFillWidth(true);
        thumbnailColumn.setAlignment(Pos.TOP_CENTER);
        thumbnailColumn.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(thumbnailList, Priority.ALWAYS);

        videoStack = new StackPane(videoView, captureInstruction, captureTimerOverlay, startCycleButton);
        videoStack.getStyleClass().add("capture-video-stack");
        videoStack.setMinSize(640, 480);
        videoStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(captureInstruction, Pos.TOP_CENTER);
        StackPane.setMargin(captureInstruction, new Insets(96, 32, 0, 32));
        StackPane.setAlignment(captureTimerOverlay, Pos.CENTER);
        StackPane.setAlignment(startCycleButton, Pos.CENTER);

        videoClip = new Rectangle();
        videoStack.setClip(videoClip);

        captureHeaderLabel = new Label();
        captureHeaderLabel.getStyleClass().add("capture-header");
        captureHeaderLabel.setWrapText(true);

        BorderPane overlay = new BorderPane();
        overlay.setPickOnBounds(false);
        overlay.setTop(captureHeaderLabel);
        BorderPane.setAlignment(captureHeaderLabel, Pos.TOP_LEFT);
        BorderPane.setMargin(captureHeaderLabel, new Insets(24, 32, 12, 32));
        overlay.setRight(thumbnailColumn);
        BorderPane.setAlignment(thumbnailColumn, Pos.TOP_RIGHT);
        BorderPane.setMargin(thumbnailColumn, new Insets(32, 32, 32, 0));
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane root = new StackPane(videoStack, overlay);
        root.getStyleClass().add("capture-root");
        StackPane.setAlignment(videoStack, Pos.CENTER);
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1080, 760);
        applyStyles(scene);
        scene.widthProperty().addListener((obs, old, val) -> adjustVideoFit(scene));
        scene.heightProperty().addListener((obs, old, val) -> adjustVideoFit(scene));
        Platform.runLater(() -> adjustVideoFit(scene));
        return scene;
    }

    private void loadAvailableWebcams() {
        webcamBox.getItems().clear();
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            webcamBox.getItems().addAll(webcams);
            if (!webcams.isEmpty()) {
                webcamBox.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            showError("Unable to enumerate available cameras.", ex);
        }
    }

    private void loadAvailablePrinters() {
        if (printerBox == null) {
            return;
        }
        try {
            List<Printer> printers = new ArrayList<>(Printer.getAllPrinters());
            printerBox.getItems().setAll(printers);

            Printer preferred = selectedPrinter;
            if (preferred != null && !printers.contains(preferred)) {
                preferred = null;
            }
            if (preferred == null) {
                Printer defaultPrinter = Printer.getDefaultPrinter();
                if (defaultPrinter != null && printers.contains(defaultPrinter)) {
                    preferred = defaultPrinter;
                }
            }

            if (preferred != null) {
                printerBox.getSelectionModel().select(preferred);
                selectedPrinter = preferred;
            } else if (!printers.isEmpty()) {
                printerBox.getSelectionModel().selectFirst();
                selectedPrinter = printerBox.getSelectionModel().getSelectedItem();
            } else {
                printerBox.getSelectionModel().clearSelection();
                selectedPrinter = null;
            }
        } catch (Exception ex) {
            showError("Unable to enumerate printers.", ex);
        }
    }

    private void beginSession() {
        if (captureInProgress) {
            return;
        }

        if (frameToggleGroup.getSelectedToggle() == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select a frame style before starting the session.").showAndWait();
            return;
        }

        Webcam selectedCamera = webcamBox.getSelectionModel().getSelectedItem();
        if (selectedCamera == null) {
            new Alert(Alert.AlertType.INFORMATION, "Choose a camera to start capturing.").showAndWait();
            return;
        }

        selectedTheme = (FrameTheme) frameToggleGroup.getSelectedToggle().getUserData();
        selectedTextStyle = textStyleBox != null && textStyleBox.getValue() != null ? textStyleBox.getValue()
                : TextStyle.SCRIPT;
        selectedPrinter = printerBox != null ? printerBox.getSelectionModel().getSelectedItem() : null;
        try {
            ensureSaveDirectoryExists();
        } catch (RuntimeException ex) {
            showError("Unable to access the save folder.", ex);
            return;
        }

        primaryStage.setScene(captureScene);

        if (!openWebcam(selectedCamera)) {
            primaryStage.setScene(setupScene);
            return;
        }

        showReadyState("Press \"Begin Session\" when you're ready. Five photos will be taken automatically.", true);
    }

    private void showReadyState(String message, boolean clearThumbnails) {
        captureInstruction.setText(message);
        captureInstruction.setVisible(true);
        captureTimerOverlay.setVisible(false);
        captureTimerOverlay.setText("");
        captureHeaderLabel.setText(selectedTheme.formatHeadline(eventField.getText()));

        if (clearThumbnails) {
            capturedImages.clear();
            for (int i = 0; i < captureThumbnails.length; i++) {
                if (captureThumbnails[i] != null) {
                    captureThumbnails[i].setImage(null);
                }
                if (captureThumbnailBadges[i] != null) {
                    captureThumbnailBadges[i].setText("#" + (i + 1));
                    captureThumbnailBadges[i].setVisible(true);
                }
                if (captureThumbnailFrames[i] != null) {
                    captureThumbnailFrames[i].getStyleClass().remove("capture-thumb-active");
                    captureThumbnailFrames[i].getStyleClass().remove("capture-thumb-complete");
                }
            }
        } else {
            for (StackPane frame : captureThumbnailFrames) {
                if (frame != null) {
                    frame.getStyleClass().remove("capture-thumb-active");
                }
            }
        }

        if (startCycleButton != null) {
            startCycleButton.setDisable(false);
            startCycleButton.setText("Begin Session");
            startCycleButton.setVisible(true);
            startCycleButton.setManaged(true);
        }
    }

    private boolean openWebcam(Webcam selected) {
        stopWebcam();
        try {
            selected.setViewSize(WebcamResolution.VGA.getSize());
            selected.open(true);
            webcam = selected;
        } catch (Exception ex) {
            showError("Unable to open the selected camera.", ex);
            webcam = null;
            return false;
        }

        Thread streamThread = new Thread(() -> {
            while (webcam != null && webcam == selected && webcam.isOpen()) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        Image fxImage = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> {
                            videoView.setImage(fxImage);
                            captureInstruction.setVisible(false);
                            if (captureScene != null) {
                                adjustVideoFit(captureScene);
                            }
                        });
                    }
                    Thread.sleep(33);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    showError("Lost connection to the camera.", ex);
                    break;
                }
            }
        });
        streamThread.setDaemon(true);
        streamThread.start();

        return true;
    }

    private void stopWebcam() {
        if (webcam != null) {
            try {
                webcam.close();
            } catch (Exception ignored) {
            } finally {
                webcam = null;
            }
        }
    }

    private void startCaptureSequence() {
        if (webcam == null || !webcam.isOpen() || captureInProgress) {
            return;
        }
        captureInProgress = true;
        currentSessionStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        capturedImages.clear();

        if (startCycleButton != null) {
            startCycleButton.setDisable(true);
            startCycleButton.setText("Capturing...");
            startCycleButton.setVisible(false);
            startCycleButton.setManaged(false);
        }

        captureInstruction.setVisible(false);
        captureTimerOverlay.setVisible(false);
        captureTimerOverlay.setText("");
        captureHeaderLabel.setText(selectedTheme.formatHeadline(eventField.getText()));

        for (int i = 0; i < captureThumbnails.length; i++) {
            if (captureThumbnailFrames[i] != null) {
                captureThumbnailFrames[i].getStyleClass().remove("capture-thumb-active");
                captureThumbnailFrames[i].getStyleClass().remove("capture-thumb-complete");
            }
            if (captureThumbnails[i] != null) {
                captureThumbnails[i].setImage(null);
            }
            if (captureThumbnailBadges[i] != null) {
                captureThumbnailBadges[i].setText("#" + (i + 1));
                captureThumbnailBadges[i].setVisible(true);
            }
        }

        new Thread(() -> {
            boolean success = true;
            try {
                for (int i = 0; i < CAPTURE_COUNT; i++) {
                    highlightThumbnail(i);
                    runCountdownOverlay(3);

                    if (webcam == null || !webcam.isOpen()) {
                        throw new IllegalStateException("Camera disconnected.");
                    }

                    BufferedImage raw = webcam.getImage();
                    if (raw == null) {
                        throw new IllegalStateException("Failed to capture image.");
                    }

                    capturedImages.add(raw);
                    updateThumbnail(i, raw);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                success = false;
            } catch (Exception ex) {
                success = false;
                showError("Capture failed.", ex);
            } finally {
                boolean complete = success && capturedImages.size() == CAPTURE_COUNT;
                Platform.runLater(() -> finishCapture(complete));
            }
        }).start();
    }

    private void highlightThumbnail(int index) {
        Platform.runLater(() -> {
            for (StackPane frame : captureThumbnailFrames) {
                if (frame != null) {
                    frame.getStyleClass().remove("capture-thumb-active");
                }
            }
            if (index >= 0 && index < captureThumbnailFrames.length && captureThumbnailFrames[index] != null) {
                captureThumbnailFrames[index].getStyleClass().add("capture-thumb-active");
            }
            if (index >= 0 && index < captureThumbnailBadges.length && captureThumbnailBadges[index] != null) {
                captureThumbnailBadges[index].setVisible(true);
                captureThumbnailBadges[index].setText("Capturing...");
            }
        });
    }

    private void runCountdownOverlay(int seconds) throws InterruptedException {
        for (int i = seconds; i >= 1; i--) {
            int display = i;
            Platform.runLater(() -> {
                captureTimerOverlay.setVisible(true);
                captureTimerOverlay.setText(String.valueOf(display));
            });
            Thread.sleep(1000);
        }
        Platform.runLater(() -> {
            captureTimerOverlay.setVisible(false);
            captureTimerOverlay.setText("");
        });
        Thread.sleep(150);
    }

    private void updateThumbnail(int index, BufferedImage annotated) {
        WritableImage fxImage = SwingFXUtils.toFXImage(annotated, null);
        Platform.runLater(() -> {
            if (index >= 0 && index < captureThumbnails.length && captureThumbnails[index] != null) {
                captureThumbnails[index].setImage(fxImage);
            }
            if (index >= 0 && index < captureThumbnailBadges.length && captureThumbnailBadges[index] != null) {
                captureThumbnailBadges[index].setVisible(false);
            }
            if (index >= 0 && index < captureThumbnailFrames.length && captureThumbnailFrames[index] != null) {
                captureThumbnailFrames[index].getStyleClass().remove("capture-thumb-active");
                captureThumbnailFrames[index].getStyleClass().add("capture-thumb-complete");
            }
        });
    }

    private void finishCapture(boolean success) {
        captureInProgress = false;

        if (!success) {
            showReadyState("Capture interrupted. Press \"Begin Session\" to try again.", true);
            return;
        }

        if (capturedImages.size() < CAPTURE_COUNT) {
            showReadyState("Not enough photos captured. Press \"Begin Session\" to retry.", true);
            return;
        }

        WritableImage template = createTemplate();
        autoSaveAndPrint(template);
        capturedImages.clear();
        showReadyState("Thank you! Press \"Begin Session\" for another set.", false);
    }

    private void updateFrameStatementPreview() {
        frameStatementPreview.setText(selectedTheme.formatStatement(eventField.getText()));
    }

    private VBox buildField(String labelText, Node node) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(6, label, node);
        box.getStyleClass().add("field");
        return box;
    }

    private ToggleButton createFrameToggle(FrameTheme theme) {
        ToggleButton toggle = new ToggleButton();
        toggle.setUserData(theme);
        toggle.setToggleGroup(frameToggleGroup);
        toggle.getStyleClass().addAll("frame-card", theme.cardCss());
        toggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggle.setPrefWidth(220);
        toggle.setMinWidth(200);
        toggle.setPrefHeight(150);

        Label icon = new Label(theme.emoji());
        icon.getStyleClass().add("frame-card-icon");

        Label title = new Label(theme.displayName());
        title.getStyleClass().add("frame-card-title");

        Label desc = new Label(theme.description());
        desc.getStyleClass().add("frame-card-description");
        desc.setWrapText(true);

        Label statement = new Label(theme.formatStatement("Alex & Taylor"));
        statement.getStyleClass().add("frame-card-sample");
        statement.setWrapText(true);

        VBox content = new VBox(6, icon, title, desc, statement);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMaxWidth(200);

        toggle.setGraphic(content);
        return toggle;
    }

    private StackPane createThumbnailSlot(int index) {
        ImageView iv = new ImageView();
        iv.setFitWidth(240);
        iv.setFitHeight(170);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        Label badge = new Label("#" + (index + 1));
        badge.getStyleClass().add("capture-thumb-label");

        StackPane slot = new StackPane(iv, badge);
        slot.getStyleClass().add("capture-thumb-slot");
        slot.setPrefWidth(240);
        slot.setMaxWidth(Double.MAX_VALUE);
        captureThumbnails[index] = iv;
        captureThumbnailBadges[index] = badge;
        return slot;
    }

    private void chooseSaveDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Save Folder");
        if (saveDirectory != null && saveDirectory.exists()) {
            chooser.setInitialDirectory(saveDirectory);
        }
        File selected = chooser.showDialog(primaryStage);
        if (selected != null) {
            saveDirectory = selected;
            try {
                ensureSaveDirectoryExists();
            } catch (RuntimeException ex) {
                showError("Unable to access the save folder.", ex);
                return;
            }
            updateSaveLocationLabel();
        }
    }

    private void updateSaveLocationLabel() {
        if (saveLocationLabel != null) {
            saveLocationLabel.setText(saveDirectory.getAbsolutePath());
        }
    }

    private File ensureSaveDirectoryExists() {
        if (saveDirectory == null) {
            saveDirectory = new File("photos");
        }
        if (!saveDirectory.exists() && !saveDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create save folder: " + saveDirectory);
        }
        return saveDirectory;
    }

    private WritableImage createTemplate() {
        final int W = 1800;
        final int H = 1200;

        // Layout constants
        final double padding = 48; // outer padding
        final double topBarHeight = 90; // space for the top-left/right labels
        final double betweenSections = 28; // spacing between major sections
        double thumbRowHeight = 200; // height target for thumbnail row
        double thumbSpacing = 18; // gap between thumbnails

        if (capturedImages.size() < CAPTURE_COUNT) {
            throw new IllegalStateException("Not enough images captured to build template");
        }

        // ---- Canvas root ----
        Pane canvas = new Pane();
        canvas.setPrefSize(W, H);
        canvas.getStyleClass().addAll("template-canvas", selectedTheme.canvasCss(), "template-bordered");

        // ---- Prepare FX images ----
        Image[] fxImgs = new Image[CAPTURE_COUNT];
        for (int i = 0; i < CAPTURE_COUNT; i++) {
            fxImgs[i] = SwingFXUtils.toFXImage(capturedImages.get(i), null);
        }

        // ---- Derive event name (with style rules) ----
        String eventName = eventField.getText();
        if (eventName == null || eventName.isBlank()) {
            eventName = "Your Event";
        } else {
            eventName = eventName.trim();
        }
        if (selectedTextStyle == TextStyle.MODERN) {
            eventName = eventName.toUpperCase();
        }

        // ---- Top labels (event left, date right) ----
        // Event name top-left, uses your cursive/modern/classic styles from CSS
        Label eventNameLabel = new Label(eventName);
        eventNameLabel.getStyleClass().addAll("template-event-label", selectedTextStyle.cssClass());
        eventNameLabel.setLayoutX(padding + 10);
        eventNameLabel.setLayoutY(padding - 8); // slightly tuck upward

        // Date (or you can replace with any right-top text)
        String dateText = new SimpleDateFormat("MMM dd, yyyy").format(new Date());
        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("template-event-label");
        dateLabel.setTextAlignment(TextAlignment.RIGHT);
        dateLabel.setAlignment(Pos.CENTER_RIGHT);
        // give it a max width and position it on the right
        double rightLabelWidth = 320;
        dateLabel.setMaxWidth(rightLabelWidth);
        dateLabel.setPrefWidth(rightLabelWidth);
        dateLabel.setLayoutX(W - padding - rightLabelWidth - 10);
        dateLabel.setLayoutY(padding - 8);

        canvas.getChildren().addAll(eventNameLabel, dateLabel);

        // ---- Compute main photo area ----
        double mainWidth = W - padding * 2;
        // Height available after top bar + spacing + thumb row + spacing + bottom
        // padding
        double availableForMain = H - padding - topBarHeight - betweenSections - thumbRowHeight - betweenSections
                - padding;

        // Ensure a reasonable minimum height; if tight, shrink thumb row a bit
        if (availableForMain < 360) {
            thumbRowHeight = Math.max(170, thumbRowHeight - 30);
            availableForMain = H - padding - topBarHeight - betweenSections - thumbRowHeight - betweenSections
                    - padding;
        }

        double mainHeight = availableForMain;
        double mainTop = padding + topBarHeight + betweenSections;

        // ---- Large main photo (slightly smaller to avoid crowding) ----
        ImageView mainView = new ImageView(fxImgs[0]);
        mainView.setPreserveRatio(true);
        mainView.setSmooth(true);
        mainView.getStyleClass().add("template-photo");

        // scale with a little breathing room
        mainView.setFitWidth(mainWidth * 0.95);
        mainView.setFitHeight(mainHeight * 0.95);
        mainView.setLayoutX(padding + (mainWidth - mainView.getFitWidth()) / 2.0);
        mainView.setLayoutY(mainTop + (mainHeight - mainView.getFitHeight()) / 2.0);

        canvas.getChildren().add(mainView);

        // ---- Thumbnails row (1..4) under the main photo, no overlap ----
        double thumbRowTop = mainTop + mainHeight + betweenSections;
        double usableThumbWidth = mainWidth - (thumbSpacing * 3); // 4 thumbs → 3 gaps
        double thumbWidth = usableThumbWidth / 4.0;

        // Safety: if preserveRatio results in taller than thumbRowHeight, we still pin
        // Y by thumbRowTop
        for (int i = 1; i < CAPTURE_COUNT; i++) {
            ImageView thumb = new ImageView(fxImgs[i]);
            thumb.setPreserveRatio(true);
            thumb.setSmooth(true);
            thumb.getStyleClass().add("template-photo");

            // fit target box
            thumb.setFitWidth(thumbWidth);
            thumb.setFitHeight(thumbRowHeight);

            double x = padding + (thumbWidth + thumbSpacing) * (i - 1) + 10; // tiny nudge
            thumb.setLayoutX(x);
            thumb.setLayoutY(thumbRowTop);

            canvas.getChildren().add(thumb);
        }

        // ---- Snapshot ----
        WritableImage snapshot = new WritableImage(W, H);
        canvas.snapshot(new SnapshotParameters(), snapshot);
        return snapshot;
    }

    private void autoSaveAndPrint(WritableImage templateImage) {
        try {
            File dir = ensureSaveDirectoryExists();
            String baseName = sanitizeForFile(eventField.getText());
            String stamp = currentSessionStamp != null ? currentSessionStamp
                    : new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outFile = new File(dir, String.format("%s_strip_%s.jpg", baseName, stamp));
            BufferedImage bImage = SwingFXUtils.fromFXImage(templateImage, null);
            ImageIO.write(bImage, "JPEG", outFile);

            Printer printer = selectedPrinter != null ? selectedPrinter : Printer.getDefaultPrinter();
            StringBuilder message = new StringBuilder("Saved strip to: ").append(outFile.getAbsolutePath());
            if (printer != null) {
                PrinterJob job = PrinterJob.createPrinterJob(printer);
                if (job != null) {
                    Paper photoPaper = resolvePhotoPaper(printer);
                    PageLayout layout = printer.createPageLayout(photoPaper, PageOrientation.LANDSCAPE,
                            Printer.MarginType.HARDWARE_MINIMUM);
                    job.getJobSettings().setPageLayout(layout);

                    ImageView iv = new ImageView(templateImage);
                    double printableWidth = layout.getPrintableWidth();
                    double printableHeight = layout.getPrintableHeight();
                    double scale = Math.min(printableWidth / templateImage.getWidth(),
                            printableHeight / templateImage.getHeight());
                    if (scale > 1.0) {
                        scale = 1.0;
                    }
                    iv.setFitWidth(templateImage.getWidth() * scale);
                    iv.setFitHeight(templateImage.getHeight() * scale);
                    iv.setPreserveRatio(true);

                    if (job.printPage(layout, iv)) {
                        job.endJob();
                        message.append("\nPrint sent to ").append(printer.getName());
                    } else {
                        message.append("\nPrint could not be completed.");
                    }
                } else {
                    message.append("\nUnable to create printer job for ").append(printer.getName());
                }
            } else {
                message.append("\nNo printer selected or detected.");
            }
            Alert info = new Alert(Alert.AlertType.INFORMATION, message.toString());
            info.setTitle("PhotoBooth");
            info.setHeaderText("Photo strip generated");
            info.showAndWait();
        } catch (Exception ex) {
            showError("Print/save error.", ex);
        }
    }

    private Paper resolvePhotoPaper(Printer printer) {
        // Try to find a common photo paper size
        for (Paper paper : Printer.getDefaultPrinter().getPrinterAttributes().getSupportedPapers()) {
            String name = paper.getName().toLowerCase();
            if (name.contains("4x6") || name.contains("4 x 6") || name.contains("4 by 6")) {
                return paper;
            }
        }

        // Fallback to standard LETTER if no 4x6 found
        return Paper.NA_LETTER;
    }

    private void adjustVideoFit(Scene scene) {
        if (videoView == null) {
            return;
        }
        double containerWidth = Math.max(scene.getWidth(), 1);
        double containerHeight = Math.max(scene.getHeight(), 1);

        if (videoClip != null) {
            videoClip.setWidth(containerWidth);
            videoClip.setHeight(containerHeight);
        }

        Image currentImage = videoView.getImage();
        if (currentImage != null && currentImage.getWidth() > 0 && currentImage.getHeight() > 0) {
            double imageRatio = currentImage.getWidth() / currentImage.getHeight();
            double containerRatio = containerWidth / containerHeight;

            if (containerRatio > imageRatio) {
                videoView.setFitWidth(containerWidth);
                videoView.setFitHeight(containerWidth / imageRatio);
            } else {
                videoView.setFitHeight(containerHeight);
                videoView.setFitWidth(containerHeight * imageRatio);
            }
        } else {
            videoView.setFitWidth(containerWidth);
            videoView.setFitHeight(containerHeight);
        }
    }

    private String sanitizeForFile(String raw) {
        String base = (raw == null || raw.isBlank()) ? "event" : raw.trim();
        return base.replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase();
    }

    private String stylesheet() {
        if (stylesheet == null) {
            URL resource = getClass().getResource("/application.css");
            if (resource != null) {
                stylesheet = resource.toExternalForm();
            }
        }
        return stylesheet;
    }

    private void applyStyles(Scene scene) {
        String css = stylesheet();
        if (css != null && !scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    private void showError(String userMessage, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("PhotoBooth");
            alert.setHeaderText("Something went wrong");
            String details = (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank())
                    ? "\n\nDetails: " + ex.getMessage()
                    : "";
            alert.setContentText(userMessage + details);
            alert.showAndWait();
        });
    }

    private enum TextStyle {
        SCRIPT("Elegant Script", "template-event-script"),
        MODERN("Modern Sans", "template-event-modern"),
        CLASSIC("Classic Serif", "template-event-classic");

        private final String displayName;
        private final String cssClass;

        TextStyle(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        String cssClass() {
            return cssClass;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum FrameTheme {
        BIRTHDAY_CELEBRATION(
                "Birthday Celebration",
                "Sparkling confetti bursts with bold neon type for parties of all ages.",
                "frame-card-birthday",
                "template-canvas-birthday",
                "template-headline-birthday",
                "statement-bar-birthday",
                "Happy Birthday, %s!",
                "%s's Birthday Bash",
                "\uD83C\uDF89"),
        WEDDING_ELEGANCE(
                "Wedding Elegance",
                "Soft florals and metallic ribbons for timeless romance.",
                "frame-card-wedding",
                "template-canvas-wedding",
                "template-headline-wedding",
                "statement-bar-wedding",
                "Best Wishes to %s",
                "The Wedding of %s",
                "\uD83D\uDC8D"),
        BRIDE_GLAM(
                "Bride Glam",
                "Blush highlights and delicate lace accents for the bride's keepsake strip.",
                "frame-card-bride",
                "template-canvas-bride",
                "template-headline-bride",
                "statement-bar-bride",
                "Radiant Bride %s",
                "%s's Bridal Spotlight",
                "\uD83D\uDC70"),
        GROOM_CLASSIC(
                "Groom Classic",
                "Sleek midnight tones and suit-tailored lines for the groom's crew.",
                "frame-card-groom",
                "template-canvas-groom",
                "template-headline-groom",
                "statement-bar-groom",
                "Cheers to %s",
                "%s's Groom Squad",
                "\uD83E\uDD35");

        private final String displayName;
        private final String description;
        private final String cardCss;
        private final String canvasCss;
        private final String headlineCss;
        private final String statementCss;
        private final String statementFormat;
        private final String headlineFormat;
        private final String emoji;

        FrameTheme(String displayName,
                String description,
                String cardCss,
                String canvasCss,
                String headlineCss,
                String statementCss,
                String statementFormat,
                String headlineFormat,
                String emoji) {
            this.displayName = displayName;
            this.description = description;
            this.cardCss = cardCss;
            this.canvasCss = canvasCss;
            this.headlineCss = headlineCss;
            this.statementCss = statementCss;
            this.statementFormat = statementFormat;
            this.headlineFormat = headlineFormat;
            this.emoji = emoji;
        }

        String displayName() {
            return displayName;
        }

        String description() {
            return description;
        }

        String cardCss() {
            return cardCss;
        }

        String canvasCss() {
            return canvasCss;
        }

        String headlineCss() {
            return headlineCss;
        }

        String statementCss() {
            return statementCss;
        }

        String emoji() {
            return emoji;
        }

        String formatStatement(String rawEvent) {
            return statementFormat.formatted(sanitise(rawEvent));
        }

        String formatHeadline(String rawEvent) {
            return headlineFormat.formatted(sanitise(rawEvent));
        }

        private static String sanitise(String raw) {
            return (raw == null || raw.isBlank()) ? "Your Event" : raw.trim();
        }
    }

}
