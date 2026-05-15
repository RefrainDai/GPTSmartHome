package com.gptsmarthome.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {
    private final Path root;
    private final MediaConfig config;
    private MediaPlayer currentEffect;
    private MediaPlayer currentVoice;
    private double effectVolume = 0.75;

    public SoundManager() {
        this.root = ProjectPaths.root();
        this.config = loadConfig(root.resolve("config").resolve("media-map.json"));
    }

    public void playDeviceCue(String deviceId, String action, LogSink log) {
        if (deviceId == null || action == null || config.sounds == null || !config.sounds.containsKey(deviceId)) {
            return;
        }
        String file = config.sounds.get(deviceId).get(action);
        if (file == null) {
            return;
        }
        playLocal(root.resolve("assets").resolve("sounds").resolve(file), effectVolume, log);
    }

    public void playSceneCue(String scene, LogSink log) {
        playDeviceCue(scene, "on", log);
    }

    public void playAssistantVoice(String audioUrl, String backendBaseUrl, LogSink log) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return;
        }
        try {
            if (currentVoice != null) {
                currentVoice.stop();
            }
            String url = audioUrl.startsWith("http") ? audioUrl : backendBaseUrl + audioUrl;
            currentVoice = new MediaPlayer(new Media(url));
            currentVoice.setVolume(0.9);
            currentVoice.play();
        } catch (Exception ex) {
            log.log("语音回复播放失败：" + ex.getMessage());
        }
    }

    public MediaConfig.VideoEntry tvVideoConfig() {
        return config.videos == null ? null : config.videos.get("television");
    }

    public Path videoPath(String file) {
        return root.resolve("assets").resolve("videos").resolve(file);
    }

    private void playLocal(Path file, double volume, LogSink log) {
        if (!Files.exists(file)) {
            log.log("音效文件不存在，可自行放置：" + file);
            return;
        }
        try {
            if (currentEffect != null) {
                currentEffect.stop();
            }
            currentEffect = new MediaPlayer(new Media(file.toUri().toString()));
            currentEffect.setVolume(volume);
            currentEffect.play();
        } catch (Exception ex) {
            log.log("音效播放失败：" + ex.getMessage());
        }
    }

    private MediaConfig loadConfig(Path path) {
        try {
            if (Files.exists(path)) {
                return new ObjectMapper().readValue(path.toFile(), MediaConfig.class);
            }
        } catch (Exception ignored) {
        }
        return new MediaConfig();
    }

    public interface LogSink {
        void log(String message);
    }
}
