package com.gptsmarthome.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BackendClient {
    private final String baseUrl;
    private final String wsUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocketClient socket;
    private EventListener listener;

    public BackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws";
    }

    public String baseUrl() {
        return baseUrl;
    }

    public List<DeviceState> fetchDevices() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/devices"))
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .timeout(Duration.ofSeconds(4))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode devices = mapper.readTree(response.body()).path("devices");
        List<DeviceState> result = new ArrayList<>();
        for (JsonNode node : devices) {
            result.add(mapper.treeToValue(node, DeviceState.class));
        }
        return result;
    }

    public void postDeviceAction(String deviceId, String action) {
        ObjectNode body = mapper.createObjectNode();
        body.put("action", action);
        body.put("source", "frontend");
        body.set("params", mapper.createObjectNode());
        postJson("/api/devices/" + deviceId + "/action", body);
    }

    public void postTextCommand(String text) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        body.put("source", "frontend");
        postJson("/api/command/text", body);
    }

    public void postSimple(String path) {
        postJson(path, mapper.createObjectNode());
    }

    public void getDiagnostics() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/system/diagnostics"))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        notifyStatus("设备诊断失败：HTTP " + response.statusCode() + "，响应：" + response.body());
                        return;
                    }
                    notifyStatus("设备诊断结果：" + response.body());
                })
                .exceptionally(ex -> {
                    notifyStatus("设备诊断请求失败：" + ex.getMessage());
                    return null;
                });
    }

    public void connect(EventListener listener) {
        this.listener = listener;
        socket = new WebSocketClient(URI.create(wsUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                listener.onStatus("WebSocket 已连接");
            }

            @Override
            public void onMessage(String message) {
                listener.onEvent(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                listener.onStatus("WebSocket 已断开：" + reason);
            }

            @Override
            public void onError(Exception ex) {
                listener.onStatus("WebSocket 错误：" + ex.getMessage());
            }
        };
        socket.connect();
    }

    public void close() {
        if (socket != null) {
            socket.close();
        }
    }

    private void postJson(String path, JsonNode body) {
        String payload;
        try {
            payload = mapper.writeValueAsString(body);
        } catch (Exception ex) {
            notifyStatus("请求构造失败：" + ex.getMessage());
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        notifyStatus("HTTP " + response.statusCode() + " 请求失败：" + path + "，响应：" + response.body());
                    }
                })
                .exceptionally(ex -> {
                    notifyStatus("HTTP 请求失败：" + ex.getMessage());
                    return null;
                });
    }

    private void notifyStatus(String message) {
        if (listener != null) {
            listener.onStatus(message);
        }
    }

    public interface EventListener {
        void onEvent(String message);

        void onStatus(String message);
    }
}
