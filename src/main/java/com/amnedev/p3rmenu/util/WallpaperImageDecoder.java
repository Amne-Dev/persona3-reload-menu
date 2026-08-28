package com.amnedev.p3rmenu.util;

import net.minecraft.client.texture.NativeImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Decodes all supported wallpaper formats into Minecraft's native ABGR texture format. */
public final class WallpaperImageDecoder {
    private WallpaperImageDecoder() {
    }

    public static boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    public static NativeImage read(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".webp")) {
            try (InputStream input = Files.newInputStream(path)) {
                return NativeImage.read(input);
            }
        }

        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) {
            throw new IOException("No ImageIO reader accepted " + path.getFileName());
        }
        NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int abgr = (argb & 0xFF00FF00)
                        | (argb & 0x00FF0000) >>> 16
                        | (argb & 0x000000FF) << 16;
                image.setColor(x, y, abgr);
            }
        }
        return image;
    }
}
