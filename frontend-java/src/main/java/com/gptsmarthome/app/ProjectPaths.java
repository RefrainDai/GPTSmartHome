package com.gptsmarthome.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProjectPaths {
    private ProjectPaths() {
    }

    public static Path root() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("assets")) && Files.exists(cwd.resolve("config"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("assets")) && Files.exists(parent.resolve("config"))) {
            return parent;
        }
        return cwd;
    }
}
