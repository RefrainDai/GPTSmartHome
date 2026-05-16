package com.gptsmarthome.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SmartHomeApp extends Application {
    private static final String BACKEND_URL = System.getProperty("backend.url", "http://127.0.0.1:8000");
    private static final double INPUT_PREVIEW_WIDTH = 480;
    private static final double GESTURE_PREVIEW_HEIGHT = 270;
    private static final double WAVEFORM_HEIGHT = 64;
    private static final double VOLUME_BAR_WIDTH = 300;
    private static final double DEVICE_PANEL_WIDTH = 270;
    private static final double CONTROL_PANEL_WIDTH = 520;
    private static final int[][] HAND_CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {5, 9}, {9, 10}, {10, 11}, {11, 12},
            {9, 13}, {13, 14}, {14, 15}, {15, 16},
            {13, 17}, {17, 18}, {18, 19}, {19, 20},
            {0, 17}
    };

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, DeviceState> devices = new HashMap<>();
    private final Map<String, VBox> cards = new HashMap<>();
    private final Map<String, Label> cardMeta = new HashMap<>();
    private final BackendClient backend = new BackendClient(BACKEND_URL);
    private final SoundManager soundManager = new SoundManager();

    private TextArea logArea;
    private Label assistantLabel;
    private Label connectionLabel;
    private Label voiceStatusLabel;
    private Label voiceVolumeLabel;
    private Rectangle voiceVolumeFill;
    private Pane waveformPane;
    private Label gestureStatusLabel;
    private Pane gestureCanvas;
    private ImageView gestureImageView;
    private GridPane deviceGrid;
    private Pane housePane;
    private XYChart.Series<Number, Number> spectrumSeries;
    private Rectangle livingLightGlow;
    private Rectangle bedroomLightGlow;
    private Rectangle curtainShape;
    private Rectangle tvStandby;
    private StackPane tvScreen;
    private MediaPlayer tvPlayer;
    private MediaView tvView;
    private Circle acGlow;
    private Circle humidifierGlow;
    private Circle robotShape;
    private Arc lockArc;
    private RotateTransition fanSpin;
    private Line fanBladeA;
    private Line fanBladeB;
    private Sphere livingLight3d;
    private Sphere bedroomLight3d;
    private Sphere livingLightBeam3d;
    private Sphere bedroomLightBeam3d;
    private Box curtain3d;
    private Box tvScreen3d;
    private Box ac3d;
    private Box doorLock3d;
    private Cylinder humidifier3d;
    private Group humidifierSteam3d;
    private Cylinder robot3d;
    private Group fan3d;
    private RotateTransition fanSpin3d;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setTop(buildHeader());
        root.setCenter(buildMainContent());
        root.setRight(buildSidePanel());

        Scene scene = new Scene(root, 1500, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("GPTSmartHome 可视化智能语音交互控制系统");
        stage.setMinWidth(1280);
        stage.setMinHeight(760);
        stage.show();

        connectBackend();
        refreshDevices();
    }

    @Override
    public void stop() {
        backend.close();
        if (tvPlayer != null) {
            tvPlayer.stop();
        }
    }

    private HBox buildHeader() {
        VBox titleBox = new VBox(4);
        Label title = new Label("GPTSmartHome 智能管家");
        title.getStyleClass().add("title");
        Label subtitle = new Label("语音识别 · 手势控制 · LLM 指令理解 · 自定义音效 · 电视视频仿真");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        connectionLabel = new Label("后端连接中...");
        connectionLabel.getStyleClass().add("subtitle");
        HBox header = new HBox(titleBox, connectionLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private HBox buildMainContent() {
        housePane = buildHousePane();
        StackPane houseWrap = new StackPane(housePane);
        houseWrap.getStyleClass().add("glass-panel");
        houseWrap.setPadding(new Insets(18));

        deviceGrid = new GridPane();
        deviceGrid.setHgap(0);
        deviceGrid.setVgap(8);
        deviceGrid.setMaxWidth(Double.MAX_VALUE);
        ScrollPane cardsScroll = new ScrollPane(deviceGrid);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox cardsPanel = panel("虚拟设备", cardsScroll);
        cardsPanel.setPrefWidth(DEVICE_PANEL_WIDTH);
        cardsPanel.setMinWidth(DEVICE_PANEL_WIDTH);
        cardsPanel.setMaxWidth(DEVICE_PANEL_WIDTH);

        HBox main = new HBox(14, houseWrap, cardsPanel);
        HBox.setHgrow(houseWrap, Priority.ALWAYS);
        return main;
    }

    private ScrollPane buildSidePanel() {
        TextField commandField = new TextField();
        commandField.setPromptText("输入指令，例如：打开客厅灯 / 进入观影模式");
        Button sendButton = button("发送指令", true);
        sendButton.setOnAction(event -> {
            String text = commandField.getText().trim();
            if (!text.isEmpty()) {
                backend.postTextCommand(text);
                appendLog("手动指令：" + text);
                commandField.clear();
            }
        });
        commandField.setOnAction(event -> sendButton.fire());

        Button startVoice = button("开始监听", true);
        startVoice.setOnAction(event -> backend.postSimple("/api/listening/start"));
        Button stopVoice = button("停止监听", false);
        stopVoice.setOnAction(event -> backend.postSimple("/api/listening/stop"));
        Button startGesture = button("启动手势", false);
        startGesture.setOnAction(event -> backend.postSimple("/api/gesture/start"));
        Button stopGesture = button("停止手势", false);
        stopGesture.setOnAction(event -> backend.postSimple("/api/gesture/stop"));
        Button diagnostics = button("检测设备", false);
        diagnostics.setOnAction(event -> backend.getDiagnostics());

        HBox voiceControls = new HBox(10, startVoice, stopVoice);
        HBox gestureControls = new HBox(10, startGesture, stopGesture, diagnostics);

        HBox scenes = new HBox(10,
                sceneButton("回家", "我回来了"),
                sceneButton("观影", "进入观影模式"),
                sceneButton("睡眠", "我要睡觉了"));

        assistantLabel = new Label("AI 管家：等待指令。");
        assistantLabel.setWrapText(true);
        assistantLabel.setStyle("-fx-text-fill: #dff6ff; -fx-font-size: 15px; -fx-font-weight: 700;");

        VBox gesturePanel = buildGesturePanel();
        VBox voicePanel = buildVoicePanel();
        logArea = new TextArea();
        logArea.getStyleClass().add("log-area");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(170);

        VBox panel = panel("交互控制台",
                commandField,
                sendButton,
                voiceControls,
                gestureControls,
                scenes,
                assistantLabel,
                voicePanel,
                gesturePanel,
                logArea);
        panel.setPrefWidth(CONTROL_PANEL_WIDTH - 24);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefWidth(CONTROL_PANEL_WIDTH);
        scroll.setMaxWidth(CONTROL_PANEL_WIDTH);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        BorderPane.setMargin(scroll, new Insets(0, 0, 0, 16));
        return scroll;
    }

    private VBox buildVoicePanel() {
        Label title = new Label("语音输入可视化");
        title.setStyle("-fx-text-fill: #dff6ff; -fx-font-size: 14px; -fx-font-weight: 800;");
        voiceStatusLabel = new Label("麦克风：未启动");
        voiceStatusLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700;");
        voiceVolumeLabel = new Label("音量 0%");
        voiceVolumeLabel.setStyle("-fx-text-fill: #a7f3d0; -fx-font-size: 12px;");

        Rectangle volumeBack = new Rectangle(VOLUME_BAR_WIDTH, 10, Color.web("#0f172a"));
        volumeBack.setArcWidth(10);
        volumeBack.setArcHeight(10);
        volumeBack.setStroke(Color.web("#334155"));
        voiceVolumeFill = new Rectangle(0, 10, Color.web("#22c55e"));
        voiceVolumeFill.setArcWidth(10);
        voiceVolumeFill.setArcHeight(10);
        StackPane volumeBar = new StackPane(volumeBack, voiceVolumeFill);
        volumeBar.setAlignment(Pos.CENTER_LEFT);
        HBox volumeRow = new HBox(10, voiceVolumeLabel, volumeBar);
        volumeRow.setAlignment(Pos.CENTER_LEFT);

        waveformPane = new Pane();
        waveformPane.setPrefSize(INPUT_PREVIEW_WIDTH, WAVEFORM_HEIGHT);
        waveformPane.setMinHeight(WAVEFORM_HEIGHT);
        waveformPane.setStyle("-fx-background-color: rgba(2, 6, 23, 0.58); -fx-background-radius: 14px; -fx-border-color: rgba(125, 211, 252, 0.22); -fx-border-radius: 14px;");
        drawWaveformPlaceholder();

        LineChart<Number, Number> chart = buildSpectrumChart();
        return new VBox(8, title, voiceStatusLabel, volumeRow, waveformPane, chart);
    }

    private VBox buildGesturePanel() {
        Label title = new Label("手势识别预览");
        title.setStyle("-fx-text-fill: #dff6ff; -fx-font-size: 14px; -fx-font-weight: 800;");
        gestureStatusLabel = new Label("当前手势：未启动");
        gestureStatusLabel.setStyle("-fx-text-fill: #86efac; -fx-font-size: 13px; -fx-font-weight: 700;");
        StackPane preview = new StackPane();
        preview.setPrefSize(INPUT_PREVIEW_WIDTH, GESTURE_PREVIEW_HEIGHT);
        preview.setMinHeight(GESTURE_PREVIEW_HEIGHT);
        preview.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72); -fx-background-radius: 16px; -fx-border-color: rgba(34, 211, 238, 0.25); -fx-border-radius: 16px;");
        gestureImageView = new ImageView();
        gestureImageView.setFitWidth(INPUT_PREVIEW_WIDTH);
        gestureImageView.setFitHeight(GESTURE_PREVIEW_HEIGHT);
        gestureImageView.setPreserveRatio(false);
        gestureCanvas = new Pane();
        gestureCanvas.setPrefSize(INPUT_PREVIEW_WIDTH, GESTURE_PREVIEW_HEIGHT);
        gestureCanvas.setMinHeight(GESTURE_PREVIEW_HEIGHT);
        gestureCanvas.setMouseTransparent(true);
        preview.getChildren().addAll(gestureImageView, gestureCanvas);
        drawGesturePlaceholder("等待摄像头手势数据");
        return new VBox(8, title, gestureStatusLabel, preview);
    }

    private Pane buildHousePane() {
        StackPane pane = new StackPane();
        pane.setPrefSize(560, 690);
        pane.setMinSize(480, 640);

        Group world = new Group();
        world.setDepthTest(DepthTest.ENABLE);
        build3dHome(world);

        SubScene scene3d = new SubScene(world, 560, 690, true, SceneAntialiasing.BALANCED);
        scene3d.widthProperty().bind(pane.widthProperty());
        scene3d.heightProperty().bind(pane.heightProperty());
        scene3d.setFill(Color.TRANSPARENT);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(2400);
        camera.setFieldOfView(38);
        camera.setTranslateY(-230);
        camera.setTranslateZ(-780);
        camera.getTransforms().add(new Rotate(-22, Rotate.X_AXIS));
        scene3d.setCamera(camera);

        VBox legend = new VBox(5,
                sceneTag("3D 虚拟户型 · Direct3D GPU 渲染"),
                sceneTag("客厅 / 卧室 / 玄关：灯光、窗帘、电视、空调、风扇、加湿器、门锁、扫地机器人"));
        legend.setMouseTransparent(true);
        legend.setPadding(new Insets(14));
        legend.setStyle("-fx-background-color: rgba(2, 6, 23, 0.42); -fx-background-radius: 16px;");
        StackPane.setAlignment(legend, Pos.TOP_LEFT);

        pane.getChildren().addAll(scene3d, legend);
        return pane;
    }

    private void build3dHome(Group world) {
        PhongMaterial floor = material("#172033");
        PhongMaterial wall = material("#20304a");
        PhongMaterial wallAccent = material("#26405d");
        PhongMaterial trim = material("#38bdf8");
        PhongMaterial device = material("#94a3b8");

        AmbientLight ambient = new AmbientLight(Color.web("#7289a8"));
        world.getChildren().add(ambient);
        world.getChildren().add(box(680, 12, 520, 0, 158, 40, floor));
        world.getChildren().add(box(680, 260, 12, 0, 25, 300, wall));
        world.getChildren().add(box(12, 260, 520, -340, 25, 40, wall));
        world.getChildren().add(box(12, 260, 520, 340, 25, 40, wall));
        world.getChildren().add(box(420, 8, 8, -110, 154, 30, trim));
        world.getChildren().add(box(8, 12, 500, 40, 154, 38, wallAccent));
        world.getChildren().add(box(260, 8, 8, 190, 154, -90, trim));

        livingLightBeam3d = sphere(90, -180, -34, 180, material("#fde68a"));
        livingLightBeam3d.setOpacity(0.12);
        livingLight3d = sphere(20, -180, -118, 180, material("#475569"));
        bedroomLightBeam3d = sphere(72, 175, -34, 180, material("#f0abfc"));
        bedroomLightBeam3d.setOpacity(0.10);
        bedroomLight3d = sphere(18, 175, -118, 180, material("#475569"));

        tvScreen3d = box(138, 82, 8, -235, -18, 292, material("#020617"));
        Box tvFrame3d = box(154, 96, 12, -235, -18, 298, material("#0f172a"));
        ac3d = box(112, 34, 22, -84, -92, 286, material("#cbd5e1"));
        curtain3d = box(152, 44, 10, 135, -64, 292, material("#38bdf8"));

        fan3d = buildFan3d();
        fan3d.setTranslateX(-28);
        fan3d.setTranslateY(22);
        fan3d.setTranslateZ(286);
        fanSpin3d = new RotateTransition(Duration.seconds(0.75), fan3d);
        fanSpin3d.setAxis(Rotate.Z_AXIS);
        fanSpin3d.setByAngle(360);
        fanSpin3d.setCycleCount(Animation.INDEFINITE);

        humidifier3d = cylinder(24, 70, 150, 116, 25, device);
        humidifierSteam3d = new Group(
                sphere(10, 140, 58, 25, material("#a7f3d0")),
                sphere(8, 158, 42, 20, material("#a7f3d0")),
                sphere(7, 136, 30, 15, material("#a7f3d0")));
        humidifierSteam3d.setOpacity(0.34);
        humidifierSteam3d.setVisible(false);

        Box door = box(92, 158, 10, 260, 80, -212, material("#1e293b"));
        doorLock3d = box(18, 28, 16, 230, 82, -220, material("#22c55e"));
        robot3d = cylinder(36, 16, 72, 148, -118, material("#94a3b8"));

        world.getChildren().addAll(livingLightBeam3d, livingLight3d, bedroomLightBeam3d, bedroomLight3d,
                tvFrame3d, tvScreen3d, ac3d, curtain3d, fan3d, humidifier3d, humidifierSteam3d, door,
                doorLock3d, robot3d);
        add3dLabels(world);
    }

    private Group buildFan3d() {
        Group group = new Group();
        PhongMaterial blade = material("#c7d2fe");
        Box bladeA = box(104, 8, 5, 0, 0, 0, blade);
        Box bladeB = box(8, 104, 5, 0, 0, 0, blade);
        Cylinder hub = cylinder(11, 12, 0, 0, -1, material("#67e8f9"));
        hub.setRotationAxis(Rotate.X_AXIS);
        hub.setRotate(90);
        group.getChildren().addAll(bladeA, bladeB, hub);
        return group;
    }

    private void add3dLabels(Group world) {
        world.getChildren().addAll(
                roomPillar("客厅", -260, 132, 0),
                roomPillar("卧室", 170, 132, 10),
                roomPillar("玄关", 230, 132, -150));
    }

    private Group roomPillar(String name, double x, double y, double z) {
        Group group = new Group();
        Cylinder marker = cylinder(8, 42, x, y, z, material("#22d3ee"));
        Label label = new Label(name);
        label.setTextFill(Color.web("#e0f2fe"));
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
        label.setTranslateX(x - 22);
        label.setTranslateY(y - 58);
        label.setTranslateZ(z - 8);
        group.getChildren().addAll(marker, label);
        return group;
    }

    private Label sceneTag(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#dff6ff"));
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
        return label;
    }

    private Box box(double width, double height, double depth, double x, double y, double z, PhongMaterial material) {
        Box box = new Box(width, height, depth);
        place3d(box, x, y, z, material);
        return box;
    }

    private Cylinder cylinder(double radius, double height, double x, double y, double z, PhongMaterial material) {
        Cylinder cylinder = new Cylinder(radius, height);
        place3d(cylinder, x, y, z, material);
        return cylinder;
    }

    private Sphere sphere(double radius, double x, double y, double z, PhongMaterial material) {
        Sphere sphere = new Sphere(radius);
        place3d(sphere, x, y, z, material);
        return sphere;
    }

    private void place3d(Shape3D shape, double x, double y, double z, PhongMaterial material) {
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        shape.setMaterial(material);
    }

    private PhongMaterial material(String color) {
        PhongMaterial material = new PhongMaterial(Color.web(color));
        material.setSpecularColor(Color.web("#e0f2fe", 0.28));
        return material;
    }

    private Rectangle room(double x, double y, double w, double h) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setArcWidth(26);
        rect.setArcHeight(26);
        rect.setFill(Color.web("#0f1b31", 0.76));
        rect.setStroke(Color.web("#2dd4bf", 0.32));
        rect.setStrokeWidth(1.5);
        return rect;
    }

    private Label roomLabel(double x, double y, String name) {
        Label label = new Label(name);
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setTextFill(Color.web("#bfdbfe"));
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
        return label;
    }

    private Rectangle glowRect(double x, double y, double w, double h, Color color) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setArcWidth(36);
        rect.setArcHeight(36);
        rect.setFill(color);
        rect.setEffect(new DropShadow(30, color));
        rect.setOpacity(0.15);
        return rect;
    }

    private LineChart<Number, Number> buildSpectrumChart() {
        NumberAxis xAxis = new NumberAxis(0, 63, 8);
        NumberAxis yAxis = new NumberAxis(0, 1, 0.2);
        xAxis.setTickLabelsVisible(false);
        yAxis.setTickLabelsVisible(false);
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(130);
        spectrumSeries = new XYChart.Series<>();
        for (int i = 0; i < 64; i++) {
            spectrumSeries.getData().add(new XYChart.Data<>(i, 0));
        }
        chart.setData(FXCollections.observableArrayList(spectrumSeries));
        return chart;
    }

    private VBox panel(String title, javafx.scene.Node... children) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #e0f2fe; -fx-font-size: 18px; -fx-font-weight: 800;");
        VBox box = new VBox(14, label);
        box.getChildren().addAll(children);
        box.getStyleClass().add("glass-panel");
        box.setPadding(new Insets(18));
        return box;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "primary-button" : "secondary-button");
        return button;
    }

    private Button sceneButton(String label, String command) {
        Button button = button(label, false);
        button.setOnAction(event -> {
            backend.postTextCommand(command);
            appendLog("场景指令：" + command);
        });
        return button;
    }

    private void connectBackend() {
        backend.connect(new BackendClient.EventListener() {
            @Override
            public void onEvent(String message) {
                Platform.runLater(() -> handleEvent(message));
            }

            @Override
            public void onStatus(String message) {
                Platform.runLater(() -> {
                    connectionLabel.setText(message);
                    appendLog(message);
                });
            }
        });
    }

    private void refreshDevices() {
        Thread thread = new Thread(() -> {
            try {
                List<DeviceState> fetched = backend.fetchDevices();
                Platform.runLater(() -> {
                    fetched.forEach(device -> devices.put(device.id, device));
                    renderDeviceCards();
                    updateHouse();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> appendLog("后端设备列表获取失败：" + readableError(ex)));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDeviceCards() {
        deviceGrid.getChildren().clear();
        cards.clear();
        cardMeta.clear();
        int index = 0;
        for (DeviceState device : devices.values()) {
            VBox card = new VBox(6);
            card.getStyleClass().add("device-card");
            card.setMaxWidth(Double.MAX_VALUE);
            Label name = new Label(device.name);
            name.getStyleClass().add("device-name");
            Label meta = new Label(device.room + " · " + device.summary());
            meta.getStyleClass().add("device-meta");
            card.getChildren().addAll(name, meta);
            card.setOnMouseClicked(event -> backend.postDeviceAction(device.id, "toggle"));
            cards.put(device.id, card);
            cardMeta.put(device.id, meta);
            deviceGrid.add(card, 0, index);
            GridPane.setHgrow(card, Priority.ALWAYS);
            index++;
        }
        refreshCards();
    }

    private void refreshCards() {
        devices.values().forEach(device -> {
            VBox card = cards.get(device.id);
            Label meta = cardMeta.get(device.id);
            if (card == null || meta == null) {
                return;
            }
            card.getStyleClass().remove("device-card-on");
            if (device.is_on || ("lock".equals(device.type) && !Boolean.TRUE.equals(device.locked))) {
                card.getStyleClass().add("device-card-on");
            }
            meta.setText(device.room + " · " + device.summary());
        });
    }

    private void handleEvent(String message) {
        try {
            JsonNode root = mapper.readTree(message);
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");
            switch (type) {
                case "snapshot" -> {
                    for (JsonNode node : payload.path("devices")) {
                        DeviceState device = mapper.treeToValue(node, DeviceState.class);
                        devices.put(device.id, device);
                    }
                    renderDeviceCards();
                    updateHouse();
                }
                case "device_updated" -> {
                    DeviceState device = mapper.treeToValue(payload.path("device"), DeviceState.class);
                    String action = payload.path("action").asText(device.is_on ? "on" : "off");
                    if ("toggle".equals(action)) {
                        action = device.is_on ? "on" : "off";
                    }
                    devices.put(device.id, device);
                    if (cards.isEmpty()) {
                        renderDeviceCards();
                    }
                    refreshCards();
                    updateHouse();
                    soundManager.playDeviceCue(device.id, actionForSound(device, action), this::appendLog);
                    if (action.startsWith("scene_")) {
                        soundManager.playSceneCue(action, this::appendLog);
                    }
                    appendLog(device.name + " -> " + device.summary());
                }
                case "ai_reply" -> {
                    String text = payload.path("text").asText();
                    assistantLabel.setText("AI 管家：" + text);
                    soundManager.playAssistantVoice(payload.path("audio_url").asText(null), backend.baseUrl(), this::appendLog);
                }
                case "voice_status" -> updateVoiceStatus(payload);
                case "voice_frame" -> updateVoiceFrame(payload);
                case "gesture_status" -> updateGestureStatus(payload);
                case "spectrum" -> updateSpectrum(payload.path("values"));
                case "gesture_frame" -> updateGestureFrame(payload);
                case "voice_text" -> appendLog("识别文本：" + payload.path("text").asText());
                case "gesture" -> appendLog("识别手势：" + payload.path("name").asText());
                case "log" -> appendLog("[" + payload.path("source").asText() + "] " + payload.path("message").asText());
                default -> {
                }
            }
        } catch (Exception ex) {
            appendLog("事件解析失败：" + ex.getMessage());
        }
    }

    private void updateGestureFrame(JsonNode payload) {
        String name = payload.path("name").asText("none");
        gestureStatusLabel.setText("当前手势：" + displayGestureName(name));
        updateGestureImage(payload.path("image").asText(null));
        JsonNode landmarks = payload.path("landmarks");
        if (!landmarks.isArray() || landmarks.isEmpty()) {
            drawGesturePlaceholder("未检测到手部，请把手放入识别区域");
            return;
        }

        double width = gestureCanvas.getWidth() > 20 ? gestureCanvas.getWidth() : INPUT_PREVIEW_WIDTH;
        double height = gestureCanvas.getHeight() > 20 ? gestureCanvas.getHeight() : GESTURE_PREVIEW_HEIGHT;
        gestureCanvas.getChildren().clear();

        Rectangle guide = new Rectangle(10, 10, width - 20, height - 20);
        guide.setArcWidth(18);
        guide.setArcHeight(18);
        guide.setFill(Color.TRANSPARENT);
        guide.setStroke(Color.web("#334155"));
        guide.getStrokeDashArray().addAll(8.0, 7.0);
        gestureCanvas.getChildren().add(guide);

        JsonNode bbox = payload.path("bbox");
        if (bbox.isObject()) {
            Rectangle box = new Rectangle(
                    bbox.path("x").asDouble() * width,
                    bbox.path("y").asDouble() * height,
                    Math.max(18, bbox.path("width").asDouble() * width),
                    Math.max(18, bbox.path("height").asDouble() * height));
            box.setArcWidth(14);
            box.setArcHeight(14);
            box.setFill(Color.web("#22d3ee", 0.08));
            box.setStroke(Color.web("#22d3ee"));
            box.setStrokeWidth(2.4);
            gestureCanvas.getChildren().add(box);
        }

        for (int[] connection : HAND_CONNECTIONS) {
            if (connection[0] >= landmarks.size() || connection[1] >= landmarks.size()) {
                continue;
            }
            JsonNode a = landmarks.get(connection[0]);
            JsonNode b = landmarks.get(connection[1]);
            Line line = new Line(
                    a.path("x").asDouble() * width,
                    a.path("y").asDouble() * height,
                    b.path("x").asDouble() * width,
                    b.path("y").asDouble() * height);
            line.setStroke(Color.web("#67e8f9"));
            line.setStrokeWidth(2.2);
            gestureCanvas.getChildren().add(line);
        }

        for (JsonNode point : landmarks) {
            Circle dot = new Circle(point.path("x").asDouble() * width, point.path("y").asDouble() * height, 3.4);
            dot.setFill(Color.web("#a7f3d0"));
            dot.setStroke(Color.web("#ecfeff"));
            dot.setStrokeWidth(0.8);
            gestureCanvas.getChildren().add(dot);
        }
    }

    private void updateGestureStatus(JsonNode payload) {
        boolean running = payload.path("running").asBoolean(false);
        String state = payload.path("state").asText("stopped");
        if (!running && "error".equals(state)) {
            gestureStatusLabel.setText("当前手势：摄像头/模型启动失败");
            return;
        }
        if (!running) {
            gestureStatusLabel.setText("当前手势：未启动");
            return;
        }
        if ("starting".equals(state)) {
            gestureStatusLabel.setText("当前手势：正在打开摄像头");
        }
    }

    private void drawGesturePlaceholder(String message) {
        if (gestureCanvas == null) {
            return;
        }
        double width = gestureCanvas.getWidth() > 20 ? gestureCanvas.getWidth() : INPUT_PREVIEW_WIDTH;
        double height = gestureCanvas.getHeight() > 20 ? gestureCanvas.getHeight() : GESTURE_PREVIEW_HEIGHT;
        gestureCanvas.getChildren().clear();
        Rectangle guide = new Rectangle(10, 10, width - 20, height - 20);
        guide.setArcWidth(18);
        guide.setArcHeight(18);
        guide.setFill(Color.TRANSPARENT);
        guide.setStroke(Color.web("#334155"));
        guide.getStrokeDashArray().addAll(8.0, 7.0);
        Label label = new Label(message);
        label.setTextFill(Color.web("#94a3b8"));
        label.setLayoutX(34);
        label.setLayoutY(height / 2 - 10);
        gestureCanvas.getChildren().addAll(guide, label);
    }

    private void updateGestureImage(String encodedImage) {
        if (encodedImage == null || encodedImage.isBlank() || gestureImageView == null) {
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedImage);
            gestureImageView.setImage(new Image(new ByteArrayInputStream(bytes)));
        } catch (Exception ex) {
            appendLog("摄像头画面解析失败：" + ex.getMessage());
        }
    }

    private void updateVoiceStatus(JsonNode payload) {
        boolean running = payload.path("running").asBoolean(false);
        String state = payload.path("state").asText("stopped");
        if (!running) {
            voiceStatusLabel.setText("麦克风：已停止");
            return;
        }
        voiceStatusLabel.setText("麦克风：" + switch (state) {
            case "recognizing" -> "正在识别语音";
            case "listening" -> "正在监听输入";
            default -> "运行中";
        });
    }

    private void updateVoiceFrame(JsonNode payload) {
        double volume = payload.path("volume").asDouble(0);
        double threshold = payload.path("threshold").asDouble(0.015);
        double normalized = Math.min(1.0, volume / Math.max(threshold * 4.0, 0.001));
        voiceVolumeFill.setWidth(VOLUME_BAR_WIDTH * normalized);
        voiceVolumeFill.setFill(normalized > 0.75 ? Color.web("#f97316") : Color.web("#22c55e"));
        voiceVolumeLabel.setText("音量 " + Math.round(normalized * 100) + "%");
        updateWaveform(payload.path("waveform"));
        updateSpectrum(payload.path("spectrum"));
    }

    private void updateWaveform(JsonNode values) {
        if (waveformPane == null || !values.isArray() || values.isEmpty()) {
            return;
        }
        double width = waveformPane.getWidth() > 20 ? waveformPane.getWidth() : INPUT_PREVIEW_WIDTH;
        double height = waveformPane.getHeight() > 20 ? waveformPane.getHeight() : WAVEFORM_HEIGHT;
        waveformPane.getChildren().clear();
        Line center = new Line(8, height / 2, width - 8, height / 2);
        center.setStroke(Color.web("#334155"));
        center.setStrokeWidth(1);
        waveformPane.getChildren().add(center);

        int count = values.size();
        double previousX = 8;
        double previousY = height / 2;
        for (int i = 0; i < count; i++) {
            double value = Math.max(-1.0, Math.min(1.0, values.get(i).asDouble()));
            double x = 8 + (width - 16) * i / Math.max(1, count - 1);
            double y = height / 2 - value * (height * 0.38);
            if (i > 0) {
                Line line = new Line(previousX, previousY, x, y);
                line.setStroke(Color.web("#38bdf8"));
                line.setStrokeWidth(1.7);
                waveformPane.getChildren().add(line);
            }
            previousX = x;
            previousY = y;
        }
    }

    private void drawWaveformPlaceholder() {
        if (waveformPane == null) {
            return;
        }
        waveformPane.getChildren().clear();
        Label label = new Label("等待麦克风输入波形");
        label.setTextFill(Color.web("#94a3b8"));
        label.setLayoutX(34);
        label.setLayoutY(WAVEFORM_HEIGHT / 2 - 10);
        waveformPane.getChildren().add(label);
    }

    private String displayGestureName(String name) {
        return switch (name) {
            case "palm" -> "手掌（打开客厅灯）";
            case "fist" -> "拳头（关闭客厅灯）";
            case "victory" -> "比耶（切换电视）";
            case "point_up" -> "举手 / 单指（切换窗帘）";
            case "unknown" -> "检测中";
            case "none" -> "未检测到";
            default -> name;
        };
    }

    private String actionForSound(DeviceState device, String action) {
        if ("off".equals(action) || "close".equals(action)) {
            return "off";
        }
        if ("set_level".equals(action) || "set_temperature".equals(action) || "set_mode".equals(action)) {
            return device.is_on ? "on" : "off";
        }
        return device.is_on ? "on" : "off";
    }

    private void updateSpectrum(JsonNode values) {
        int i = 0;
        for (JsonNode value : values) {
            if (i >= spectrumSeries.getData().size()) {
                break;
            }
            spectrumSeries.getData().get(i).setYValue(value.asDouble());
            i++;
        }
    }

    private void updateHouse() {
        DeviceState living = devices.get("living_room_light");
        DeviceState bedroom = devices.get("bedroom_light");
        DeviceState curtain = devices.get("curtain");
        DeviceState tv = devices.get("television");
        DeviceState ac = devices.get("air_conditioner");
        DeviceState fan = devices.get("fan");
        DeviceState humidifier = devices.get("humidifier");
        DeviceState lock = devices.get("door_lock");
        DeviceState robot = devices.get("robot_vacuum");

        updateLamp3d(livingLight3d, livingLightBeam3d, living != null && living.is_on, "#fde68a");
        updateLamp3d(bedroomLight3d, bedroomLightBeam3d, bedroom != null && bedroom.is_on, "#f0abfc");

        boolean curtainOpen = curtain != null && curtain.is_on;
        curtain3d.setWidth(curtainOpen ? 42 : 152);
        curtain3d.setTranslateX(curtainOpen ? 188 : 135);
        set3dMaterial(curtain3d, curtainOpen ? "#7dd3fc" : "#38bdf8");

        boolean acOn = ac != null && ac.is_on;
        set3dMaterial(ac3d, acOn ? "#22d3ee" : "#cbd5e1");
        ac3d.setTranslateZ(acOn ? 280 : 286);

        boolean humidifierOn = humidifier != null && humidifier.is_on;
        set3dMaterial(humidifier3d, humidifierOn ? "#6ee7b7" : "#94a3b8");
        humidifierSteam3d.setVisible(humidifierOn);

        boolean robotOn = robot != null && robot.is_on;
        set3dMaterial(robot3d, robotOn ? "#22d3ee" : "#94a3b8");
        robot3d.setTranslateX(robotOn ? 118 : 72);

        boolean locked = lock == null || Boolean.TRUE.equals(lock.locked);
        set3dMaterial(doorLock3d, locked ? "#22c55e" : "#f97316");

        if (fan != null && fan.is_on) {
            set3dMaterial((Shape3D) fan3d.getChildren().get(2), "#67e8f9");
            if (fanSpin3d.getStatus() != Animation.Status.RUNNING) {
                fanSpin3d.play();
            }
        } else {
            set3dMaterial((Shape3D) fan3d.getChildren().get(2), "#64748b");
            fanSpin3d.stop();
        }

        if (tv != null && tv.is_on) {
            playTvVideo();
        } else {
            stopTvVideo();
        }
    }

    private void updateLamp3d(Sphere bulb, Sphere beam, boolean on, String color) {
        if (bulb == null || beam == null) {
            return;
        }
        set3dMaterial(bulb, on ? color : "#475569");
        set3dMaterial(beam, color);
        fade(beam, on ? 0.42 : 0.10);
    }

    private void set3dMaterial(Shape3D shape, String color) {
        if (shape != null) {
            shape.setMaterial(material(color));
        }
    }

    private void fade(javafx.scene.Node node, double to) {
        FadeTransition transition = new FadeTransition(Duration.millis(260), node);
        transition.setToValue(to);
        transition.play();
    }

    private void playTvVideo() {
        set3dMaterial(tvScreen3d, "#0ea5e9");
        if (tvPlayer != null && tvPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }
        MediaConfig.VideoEntry video = soundManager.tvVideoConfig();
        if (video == null || video.on == null) {
            if (tvStandby != null) {
                tvStandby.setFill(Color.web("#0ea5e9", 0.45));
            }
            return;
        }
        Path path = soundManager.videoPath(video.on);
        if (!Files.exists(path)) {
            if (tvStandby != null) {
                tvStandby.setFill(Color.web("#0ea5e9", 0.45));
            }
            appendLog("电视视频不存在，可自行放置：" + path);
            return;
        }
        try {
            stopTvVideo();
            set3dMaterial(tvScreen3d, "#0ea5e9");
            tvPlayer = new MediaPlayer(new Media(path.toUri().toString()));
            tvPlayer.setVolume(video.volume);
            if (video.loop) {
                tvPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            }
            if (tvScreen != null) {
                tvView = new MediaView(tvPlayer);
                tvView.setFitWidth(132);
                tvView.setFitHeight(68);
                tvView.setPreserveRatio(false);
                tvScreen.getChildren().add(tvView);
            }
            tvPlayer.play();
        } catch (Exception ex) {
            appendLog("电视视频播放失败：" + ex.getMessage());
        }
    }

    private void stopTvVideo() {
        if (tvPlayer != null) {
            tvPlayer.stop();
            tvPlayer.dispose();
            tvPlayer = null;
        }
        if (tvView != null && tvScreen != null) {
            tvScreen.getChildren().remove(tvView);
            tvView = null;
        }
        if (tvStandby != null) {
            tvStandby.setFill(Color.web("#050814"));
        }
        set3dMaterial(tvScreen3d, "#020617");
    }

    private void appendLog(String message) {
        if (logArea == null) {
            return;
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + time + "] " + message + System.lineSeparator());
    }

    private String readableError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
