package com.amnedev.p3rmenu.v121;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
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

/** Owns the replaceable wallpaper layer; foreground geometry never depends on it. */
public final class WallpaperManager {
    public static final String BUILTIN_NAME = "BUILT-IN P3R WALLPAPER";

    private static final Logger LOGGER = LoggerFactory.getLogger("p3rmenu-wallpapers");
    private static final ResourceLocation BUILTIN = ResourceLocation.fromNamespaceAndPath(
            "p3rmenu", "textures/gui/title/p3r_background.png");
    private static final ResourceLocation DYNAMIC = ResourceLocation.fromNamespaceAndPath(
            "p3rmenu", "dynamic/wallpaper");
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("p3rmenu");
    private static final Path WALLPAPER_DIR = CONFIG_DIR.resolve("wallpapers");
    private static final Path SELECTION_FILE = CONFIG_DIR.resolve("wallpaper.txt");

    private static boolean initialized;
    private static boolean dynamicLoaded;
    private static String selectedName = BUILTIN_NAME;
    private static ResourceLocation activeTexture = BUILTIN;
    private static int imageWidth = 2560;
    private static int imageHeight = 1369;
    private static String status = "";

    private WallpaperManager() {
    }

    public static ResourceLocation texture() {
        ensureInitialized();
        return activeTexture;
    }

    public static int imageWidth() {
        ensureInitialized();
        return imageWidth;
    }

    public static int imageHeight() {
        ensureInitialized();
        return imageHeight;
    }

    public static Path directory() {
        ensureDirectories();
        return WALLPAPER_DIR;
    }

    public static String selectedName() {
        ensureInitialized();
        return selectedName;
    }

    public static String consumeStatus() {
        String value = status;
        status = "";
        return value;
    }

    public static List<Option> discover() {
        ensureInitialized();
        List<Option> result = new ArrayList<>();
        result.add(new Option(BUILTIN_NAME, null, BUILTIN_NAME.equals(selectedName)));
        try (var paths = Files.list(WALLPAPER_DIR)) {
            paths.filter(Files::isRegularFile)
                    .filter(WallpaperImageDecoder::supported)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(),
                            String.CASE_INSENSITIVE_ORDER))
                    .map(path -> new Option(path.getFileName().toString(), path,
                            path.getFileName().toString().equals(selectedName)))
                    .forEach(result::add);
        } catch (IOException exception) {
            status = "COULD NOT READ THE WALLPAPER FOLDER";
            LOGGER.warn("Could not scan {}", WALLPAPER_DIR, exception);
        }
        return result;
    }

    public static boolean select(Option option) {
        ensureInitialized();
        if (option.path() == null) {
            useBuiltin();
            persist();
            status = "BUILT-IN WALLPAPER SELECTED";
            return true;
        }

        Path root = WALLPAPER_DIR.toAbsolutePath().normalize();
        Path file = option.path().toAbsolutePath().normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            status = "WALLPAPER FILE IS NO LONGER AVAILABLE";
            return false;
        }

        try {
            NativeImage image = WallpaperImageDecoder.read(file);
            int width = image.getWidth();
            int height = image.getHeight();
            DynamicTexture texture = new DynamicTexture(() -> "P3R custom wallpaper", image);
            releaseDynamic();
            Minecraft.getInstance().getTextureManager().register(DYNAMIC, texture);
            dynamicLoaded = true;
            activeTexture = DYNAMIC;
            selectedName = file.getFileName().toString();
            imageWidth = width;
            imageHeight = height;
            persist();
            status = "WALLPAPER APPLIED";
            return true;
        } catch (IOException | RuntimeException exception) {
            status = "IMAGE COULD NOT BE LOADED";
            LOGGER.warn("Could not load wallpaper {}", file, exception);
            return false;
        }
    }

    public static void reloadSelection() {
        releaseDynamic();
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
                        select(new Option(candidate.getFileName().toString(), candidate, true));
                        status = "";
                        return;
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not restore the selected wallpaper", exception);
        }
        useBuiltin();
    }

    private static void useBuiltin() {
        releaseDynamic();
        activeTexture = BUILTIN;
        selectedName = BUILTIN_NAME;
        imageWidth = 2560;
        imageHeight = 1369;
    }

    private static void releaseDynamic() {
        if (dynamicLoaded) {
            Minecraft.getInstance().getTextureManager().release(DYNAMIC);
            dynamicLoaded = false;
        }
    }

    private static void ensureDirectories() {
        try {
            Files.createDirectories(WALLPAPER_DIR);
        } catch (IOException exception) {
            status = "COULD NOT CREATE THE WALLPAPER FOLDER";
            LOGGER.warn("Could not create {}", WALLPAPER_DIR, exception);
        }
    }

    private static void persist() {
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

    public record Option(String name, Path path, boolean selected) {
    }
}
