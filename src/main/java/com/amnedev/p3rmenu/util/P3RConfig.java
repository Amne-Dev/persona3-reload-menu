package com.amnedev.p3rmenu.util;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/** Persistent, deliberately small client-side configuration for P3R-only features. */
public final class P3RConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("p3rmenu-config");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("p3rmenu").resolve("settings.properties");
    private static final Properties VALUES = new Properties();

    private static boolean loaded;
    private static boolean customChat = true;

    private P3RConfig() {
    }

    public static boolean isCustomChatEnabled() {
        ensureLoaded();
        return customChat;
    }

    public static void setCustomChatEnabled(boolean enabled) {
        ensureLoaded();
        customChat = enabled;
        VALUES.setProperty("custom_chat", Boolean.toString(enabled));
        save();
    }

    public static boolean toggleCustomChat() {
        setCustomChatEnabled(!isCustomChatEnabled());
        return customChat;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Files.isRegularFile(FILE)) {
            try (InputStream input = Files.newInputStream(FILE)) {
                VALUES.load(input);
                customChat = Boolean.parseBoolean(VALUES.getProperty("custom_chat", "true"));
            } catch (IOException exception) {
                LOGGER.warn("Could not load {}", FILE, exception);
            }
        }
        VALUES.setProperty("custom_chat", Boolean.toString(customChat));
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                VALUES.store(output, "Persona 3 Reload Menu settings");
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save {}", FILE, exception);
        }
    }
}
