package com.gptsmarthome.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaConfig {
    public Map<String, Map<String, String>> sounds = new HashMap<>();
    public Map<String, VideoEntry> videos = new HashMap<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoEntry {
        public String on;
        public String scene_movie;
        public boolean loop = true;
        public double volume = 0.35;
    }
}
