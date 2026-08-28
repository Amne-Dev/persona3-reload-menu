package com.amnedev.p3rmenu.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Loads a user-selected image without coupling any foreground geometry to it. */
public final class WallpaperManager {
    public static final String BUILTIN_NAME = "BUILT-IN P3R WALLPAPER";

    private static final Logger LOGGER = LoggerFactory.getLogger("p3rmenu-wallpapers");
    private static final Identifier BUILTIN = new Identifier("p3rmenu",
            "textures/gui/title/p3r_background.png");
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("p3rmenu");
    private static final Path WALLPAPER_DIR = CONFIG_DIR.resolve("wallpapers");
    private static final Path SELECTION_FILE = CONFIG_DIR.resolve("wallpaper.txt");

    private static boolean initialized;
    private static String selectedName = BUILTIN_NAME;
    private static Identifier activeTexture = BUILTIN;
    private static Identifier dynamicTexture;
    private static String status = "";

    private WallpaperManager() {
    }

    public static Identifier getBackgroundTexture() {
        ensureInitialized();
        return activeTexture;
    }

    public static Path getWallpaperDirectory() {
        ensureDirectories();
        return WALLPAPER_DIR;
    }

    public static String getSelectedName() {
        ensureInitialized();
        return selectedName;
    }

    public static String consumeStatus() {
        String value = status;
        status = "";
        return value;
    }

    public static List<WallpaperOption> discover() {
        ensureInitialized();
        List<WallpaperOption> result = new ArrayList<>();
        result.add(new WallpaperOption(BUILTIN_NAME, null, BUILTIN_NAME.equals(selectedName)));
        try (var files = Files.list(WALLPAPER_DIR)) {
            files.filter(Files::isRegularFile)
                    .filter(WallpaperImageDecoder::isSupported)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(),
                            String.CASE_INSENSITIVE_ORDER))
                    .map(path -> new WallpaperOption(path.getFileName().toString(), path,
                            path.getFileName().toString().equals(selectedName)))
                    .forEach(result::add);
        } catch (IOException exception) {
            status = "COULD NOT READ THE WALLPAPER FOLDER";
            LOGGER.warn("Could not scan {}", WALLPAPER_DIR, exception);
        }
        return result;
    }

    public static boolean select(WallpaperOption option) {
        ensureInitialized();
        if (option.path() == null) {
            releaseDynamicTexture();
            selectedName = BUILTIN_NAME;
            activeTexture = BUILTIN;
            persistSelection();
            status = "BUILT-IN WALLPAPER SELECTED";
            return true;
        }

        Path normalized = option.path().toAbsolutePath().normalize();
        Path root = WALLPAPER_DIR.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || !Files.isRegularFile(normalized)) {
            status = "WALLPAPER FILE IS NO LONGER AVAILABLE";
            return false;
        }

        try {
            NativeImage image = WallpaperImageDecoder.read(normalized);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            releaseDynamicTexture();
            MinecraftClient client = MinecraftClient.getInstance();
            dynamicTexture = client.getTextureManager()
                    .registerDynamicTexture("p3rmenu_wallpaper", texture);
            activeTexture = dynamicTexture;
            selectedName = normalized.getFileName().toString();
            persistSelection();
            status = "WALLPAPER APPLIED";
            return true;
        } catch (IOException | RuntimeException exception) {
            status = "IMAGE COULD NOT BE LOADED";
            LOGGER.warn("Could not load wallpaper {}", normalized, exception);
            return false;
        }
    }

    public static void reloadSelection() {
        releaseDynamicTexture();
        initialized = false;
        ensureInitialized();
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ensureDirectories();
        try {
            if (Files.isRegularFile(SELECTION_FILE)) {
                String saved = Files.readString(SELECTION_FILE, StandardCharsets.UTF_8).strip();
                if (!saved.isBlank() && !BUILTIN_NAME.equals(saved)) {
                    Path candidate = WALLPAPER_DIR.resolve(Path.of(saved).getFileName()).normalize();
                    if (candidate.startsWith(WALLPAPER_DIR.normalize()) && Files.isRegularFile(candidate)) {
                        select(new WallpaperOption(candidate.getFileName().toString(), candidate, true));
                        status = "";
                        return;
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not restore the selected wallpaper", exception);
        }
        releaseDynamicTexture();
        selectedName = BUILTIN_NAME;
        activeTexture = BUILTIN;
    }

    private static void ensureDirectories() {
        try {
            Files.createDirectories(WALLPAPER_DIR);
        } catch (IOException exception) {
            status = "COULD NOT CREATE THE WALLPAPER FOLDER";
            LOGGER.warn("Could not create {}", WALLPAPER_DIR, exception);
        }
    }

    private static void persistSelection() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(SELECTION_FILE, selectedName, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            status = "WALLPAPER APPLIED, BUT THE CHOICE COULD NOT BE SAVED";
            LOGGER.warn("Could not save wallpaper selection", exception);
        }
    }

    private static void releaseDynamicTexture() {
        if (dynamicTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(dynamicTexture);
            dynamicTexture = null;
        }
    }

    public record WallpaperOption(String name, Path path, boolean selected) {
    }
}
