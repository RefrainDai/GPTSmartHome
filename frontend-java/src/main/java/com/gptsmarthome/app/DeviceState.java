package com.gptsmarthome.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceState {
    public String id;
    public String name;
    public String type;
    public String room;
    public boolean is_on;
    public Integer level;
    public Integer temperature;
    public String mode;
    public Boolean locked;
    public String updated_at;

    public String summary() {
        if ("climate".equals(type) && temperature != null) {
            return (is_on ? "运行" : "关闭") + " · " + temperature + "°C";
        }
        if (("light".equals(type) || "fan".equals(type) || "humidifier".equals(type) || "curtain".equals(type)) && level != null) {
            return (is_on ? "开启" : "关闭") + " · " + level + "%";
        }
        if ("lock".equals(type)) {
            return Boolean.TRUE.equals(locked) ? "已上锁" : "已解锁";
        }
        return is_on ? "开启" : "关闭";
    }
}
