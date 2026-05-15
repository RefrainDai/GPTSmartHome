package com.gptsmarthome.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class BackendClient {
    private final String baseUrl;
    private final String wsUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocketClient socket;

    public BackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws";
    }

    public String baseUrl() {
        return baseUrl;
    }

    public List<DeviceState> fetchDevices() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/devices")).GET().timeout(Duration.ofSeconds(4)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode devices = mapper.readTree(response.body()).path("devices");
        List<DeviceState> result = new ArrayList<>();
        for (JsonNode node : devices) {
            result.add(mapper.treeToValue(node, DeviceState.class));
        }
        return result;
    }

    public void postDeviceAction(String deviceId, String action) {
        postJson("/api/devices/" + deviceId + "/action", "{\"action\":\"" + action + "\",\"source\":\"frontend\"}");
    }

    public void postTextCommand(String text) {
        String safe = text.replace("\\", "\\\\").replace("\"", "\\\"");
        postJson("/api/command/text", "{\"text\":\"" + safe + "\",\"source\":\"frontend\"}");
    }

    public void postSimple(String path) {
        postJson(path, "{}");
    }

    public void connect(EventListener listener) {
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

    private void postJson(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public interface EventListener {
        void onEvent(String message);

        void onStatus(String message);
    }
}
