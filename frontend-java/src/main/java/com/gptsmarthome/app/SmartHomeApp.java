package com.gptsmarthome.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
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
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SmartHomeApp extends Application {
    private static final String BACKEND_URL = System.getProperty("backend.url", "http://127.0.0.1:8000");

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, DeviceState> devices = new HashMap<>();
    private final Map<String, VBox> cards = new HashMap<>();
    private final Map<String, Label> cardMeta = new HashMap<>();
    private final BackendClient backend = new BackendClient(BACKEND_URL);
    private final SoundManager soundManager = new SoundManager();

    private TextArea logArea;
    private Label assistantLabel;
    private Label connectionLabel;
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

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setTop(buildHeader());
        root.setCenter(buildMainContent());
        root.setRight(buildSidePanel());

        Scene scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("GPTSmartHome 可视化智能语音交互控制系统");
        stage.setMinWidth(1160);
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
        deviceGrid.setHgap(12);
        deviceGrid.setVgap(12);
        ScrollPane cardsScroll = new ScrollPane(deviceGrid);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox cardsPanel = panel("虚拟设备", cardsScroll);
        cardsPanel.setPrefWidth(390);

        HBox main = new HBox(18, houseWrap, cardsPanel);
        HBox.setHgrow(houseWrap, Priority.ALWAYS);
        return main;
    }

    private VBox buildSidePanel() {
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

        HBox voiceControls = new HBox(10, startVoice, stopVoice);
        HBox gestureControls = new HBox(10, startGesture, stopGesture);

        HBox scenes = new HBox(10,
                sceneButton("回家", "我回来了"),
                sceneButton("观影", "进入观影模式"),
                sceneButton("睡眠", "我要睡觉了"));

        assistantLabel = new Label("AI 管家：等待指令。");
        assistantLabel.setWrapText(true);
        assistantLabel.setStyle("-fx-text-fill: #dff6ff; -fx-font-size: 15px; -fx-font-weight: 700;");

        LineChart<Number, Number> chart = buildSpectrumChart();
        logArea = new TextArea();
        logArea.getStyleClass().add("log-area");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(240);

        VBox panel = panel("交互控制台",
                commandField,
                sendButton,
                voiceControls,
                gestureControls,
                scenes,
                assistantLabel,
                chart,
                logArea);
        panel.setPrefWidth(400);
        BorderPane.setMargin(panel, new Insets(0, 0, 0, 18));
        return panel;
    }

    private Pane buildHousePane() {
        Pane pane = new Pane();
        pane.setPrefSize(520, 690);

        Rectangle living = room(28, 36, 455, 330);
        Rectangle bedroom = room(28, 392, 220, 240);
        Rectangle entry = room(272, 392, 210, 240);

        livingLightGlow = glowRect(58, 66, 170, 90, Color.web("#fde68a", 0.22));
        bedroomLightGlow = glowRect(58, 425, 135, 72, Color.web("#f0abfc", 0.18));
        curtainShape = new Rectangle(285, 54, 170, 24);
        curtainShape.setArcWidth(18);
        curtainShape.setArcHeight(18);
        curtainShape.setFill(Color.web("#38bdf8", 0.65));

        tvScreen = new StackPane();
        tvScreen.setLayoutX(298);
        tvScreen.setLayoutY(126);
        tvScreen.setPrefSize(145, 82);
        Rectangle tvFrame = new Rectangle(145, 82, Color.web("#020617"));
        tvFrame.setArcWidth(12);
        tvFrame.setArcHeight(12);
        tvFrame.setStroke(Color.web("#60a5fa"));
        tvFrame.setStrokeWidth(2);
        tvStandby = new Rectangle(132, 68, Color.web("#050814"));
        tvStandby.setArcWidth(10);
        tvStandby.setArcHeight(10);
        Label standby = new Label("TV");
        standby.setTextFill(Color.web("#64748b"));
        standby.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        tvScreen.getChildren().addAll(tvFrame, tvStandby, standby);

        acGlow = new Circle(92, 220, 22, Color.web("#22d3ee", 0.16));
        Rectangle ac = new Rectangle(58, 206, 70, 28);
        ac.setArcWidth(14);
        ac.setArcHeight(14);
        ac.setFill(Color.web("#dbeafe"));

        fanBladeA = new Line(380, 272, 430, 272);
        fanBladeB = new Line(405, 247, 405, 297);
        fanBladeA.setStroke(Color.web("#c7d2fe"));
        fanBladeA.setStrokeWidth(5);
        fanBladeB.setStroke(Color.web("#c7d2fe"));
        fanBladeB.setStrokeWidth(5);
        Circle fanHub = new Circle(405, 272, 10, Color.web("#67e8f9"));
        Pane fanGroup = new Pane(fanBladeA, fanBladeB, fanHub);
        fanSpin = new RotateTransition(Duration.seconds(1.1), fanGroup);
        fanSpin.setByAngle(360);
        fanSpin.setCycleCount(Animation.INDEFINITE);

        humidifierGlow = new Circle(182, 555, 26, Color.web("#a7f3d0", 0.14));
        Rectangle humidifier = new Rectangle(155, 535, 54, 60);
        humidifier.setArcWidth(18);
        humidifier.setArcHeight(18);
        humidifier.setFill(Color.web("#9ca3af", 0.75));

        lockArc = new Arc(373, 470, 24, 24, 0, 180);
        lockArc.setFill(Color.TRANSPARENT);
        lockArc.setStroke(Color.web("#22c55e"));
        lockArc.setStrokeWidth(5);
        Rectangle lockBody = new Rectangle(344, 470, 58, 48);
        lockBody.setArcWidth(12);
        lockBody.setArcHeight(12);
        lockBody.setFill(Color.web("#1e293b"));
        lockBody.setStroke(Color.web("#22c55e"));

        robotShape = new Circle(375, 578, 29, Color.web("#94a3b8"));
        robotShape.setStroke(Color.web("#e2e8f0"));
        robotShape.setStrokeWidth(2);

        pane.getChildren().addAll(living, bedroom, entry, livingLightGlow, bedroomLightGlow, curtainShape, tvScreen,
                acGlow, ac, fanGroup, humidifierGlow, humidifier, lockArc, lockBody, robotShape);
        pane.getChildren().addAll(roomLabel(46, 50, "客厅"), roomLabel(46, 406, "卧室"), roomLabel(290, 406, "玄关 / 设备区"));
        return pane;
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
        chart.setPrefHeight(180);
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
                Platform.runLater(() -> appendLog("后端设备列表获取失败：" + ex.getMessage()));
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
            VBox card = new VBox(8);
            card.getStyleClass().add("device-card");
            Label name = new Label(device.name);
            name.getStyleClass().add("device-name");
            Label meta = new Label(device.room + " · " + device.summary());
            meta.getStyleClass().add("device-meta");
            card.getChildren().addAll(name, meta);
            card.setOnMouseClicked(event -> backend.postDeviceAction(device.id, "toggle"));
            cards.put(device.id, card);
            cardMeta.put(device.id, meta);
            deviceGrid.add(card, index % 2, index / 2);
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
                case "spectrum" -> updateSpectrum(payload.path("values"));
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

        fade(livingLightGlow, living != null && living.is_on ? 0.95 : 0.12);
        fade(bedroomLightGlow, bedroom != null && bedroom.is_on ? 0.82 : 0.10);
        curtainShape.setWidth(curtain != null && curtain.is_on ? 42 : 170);
        acGlow.setOpacity(ac != null && ac.is_on ? 0.85 : 0.12);
        humidifierGlow.setOpacity(humidifier != null && humidifier.is_on ? 0.85 : 0.12);
        robotShape.setFill(robot != null && robot.is_on ? Color.web("#22d3ee") : Color.web("#94a3b8"));
        lockArc.setStroke(lock != null && Boolean.TRUE.equals(lock.locked) ? Color.web("#22c55e") : Color.web("#f97316"));

        if (fan != null && fan.is_on) {
            if (fanSpin.getStatus() != Animation.Status.RUNNING) {
                fanSpin.play();
            }
        } else {
            fanSpin.stop();
        }

        if (tv != null && tv.is_on) {
            playTvVideo();
        } else {
            stopTvVideo();
        }
    }

    private void fade(javafx.scene.Node node, double to) {
        FadeTransition transition = new FadeTransition(Duration.millis(260), node);
        transition.setToValue(to);
        transition.play();
    }

    private void playTvVideo() {
        if (tvPlayer != null && tvPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }
        MediaConfig.VideoEntry video = soundManager.tvVideoConfig();
        if (video == null || video.on == null) {
            tvStandby.setFill(Color.web("#0ea5e9", 0.45));
            return;
        }
        Path path = soundManager.videoPath(video.on);
        if (!Files.exists(path)) {
            tvStandby.setFill(Color.web("#0ea5e9", 0.45));
            appendLog("电视视频不存在，可自行放置：" + path);
            return;
        }
        try {
            stopTvVideo();
            tvPlayer = new MediaPlayer(new Media(path.toUri().toString()));
            tvPlayer.setVolume(video.volume);
            if (video.loop) {
                tvPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            }
            tvView = new MediaView(tvPlayer);
            tvView.setFitWidth(132);
            tvView.setFitHeight(68);
            tvView.setPreserveRatio(false);
            tvScreen.getChildren().add(tvView);
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
        if (tvView != null) {
            tvScreen.getChildren().remove(tvView);
            tvView = null;
        }
        tvStandby.setFill(Color.web("#050814"));
    }

    private void appendLog(String message) {
        if (logArea == null) {
            return;
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + time + "] " + message + System.lineSeparator());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
