package com.amnedev.p3rmenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class P3RHelper {

    /**
     * Draws a rectangle with a skewed right edge.
     * Useful for the menu item highlight strips.
     */
    public static void drawSkewedStrip(DrawContext context, int xStart, int y, int width, int height, int skewPixels,
            int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Top Left
        bufferBuilder.vertex(matrix, xStart, y, 0).color(g, h, k, f).next();
        // Bottom Left
        bufferBuilder.vertex(matrix, xStart, y + height, 0).color(g, h, k, f).next();
        // Bottom Right (Skewed)
        bufferBuilder.vertex(matrix, xStart + width - skewPixels, y + height, 0).color(g, h, k, f).next();
        // Top Right (Skewed)
        bufferBuilder.vertex(matrix, xStart + width, y, 0).color(g, h, k, f).next();

        tessellator.draw();
        RenderSystem.disableBlend();
    }

    /**
     * Draws a fully skewed rectangle (parallelogram).
     */
    public static void drawSkewedRect(DrawContext context, int x, int y, int width, int height, int skewOffset,
            int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Top Left
        bufferBuilder.vertex(matrix, x + skewOffset, y, 0).color(r, g, b, a).next();
        // Bottom Left
        bufferBuilder.vertex(matrix, x, y + height, 0).color(r, g, b, a).next();
        // Bottom Right
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(r, g, b, a).next();
        // Top Right
        bufferBuilder.vertex(matrix, x + width + skewOffset, y, 0).color(r, g, b, a).next();

        tessellator.draw();
        RenderSystem.disableBlend();
    }

    /**
     * Draws a quad with a left-skewed edge and a flat right edge.
     * This creates a shape like "/|".
     */
    public static void drawLeftSkewedRightFlatStrip(DrawContext context, int x, int y, int width, int height, int skew,
            int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Quad:
        // Top-Left: (x + skew, y)
        // Bottom-Left: (x, y + height)
        // Bottom-Right: (x + width, y + height)
        // Top-Right: (x + width, y) <-- Flat Right Edge

        // Note: x is the "Leftmost Bottom" coordinate.

        float x1 = x + skew; // Top-Left
        float y1 = y;

        float x2 = x; // Bottom-Left
        float y2 = y + height;

        float x3 = x + width; // Bottom-Right
        float y3 = y + height;

        float x4 = x + width; // Top-Right
        float y4 = y;

        bufferBuilder.vertex(matrix, x2, y2, 0).color(g, h, k, f).next(); // BL
        bufferBuilder.vertex(matrix, x3, y3, 0).color(g, h, k, f).next(); // BR
        bufferBuilder.vertex(matrix, x4, y4, 0).color(g, h, k, f).next(); // TR
        bufferBuilder.vertex(matrix, x1, y1, 0).color(g, h, k, f).next(); // TL

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    /**
     * Draws a skewed rectangle with a gradient.
     */
    public static void drawGradientSkewedRect(DrawContext context, int x, int y, int width, int height, int skewOffset,
            int colorStart, int colorEnd) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        float alpha1 = (float) (colorStart >> 24 & 255) / 255.0F;
        float red1 = (float) (colorStart >> 16 & 255) / 255.0F;
        float green1 = (float) (colorStart >> 8 & 255) / 255.0F;
        float blue1 = (float) (colorStart & 255) / 255.0F;

        float alpha2 = (float) (colorEnd >> 24 & 255) / 255.0F;
        float red2 = (float) (colorEnd >> 16 & 255) / 255.0F;
        float green2 = (float) (colorEnd >> 8 & 255) / 255.0F;
        float blue2 = (float) (colorEnd & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // Use Gouraud shading for gradient
        // RenderSystem.shadeModel intentionally removed for 1.20+

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Top Left (Start Color)
        bufferBuilder.vertex(matrix, x + skewOffset, y, 0).color(red1, green1, blue1, alpha1).next();
        // Bottom Left (Start Color)
        bufferBuilder.vertex(matrix, x, y + height, 0).color(red1, green1, blue1, alpha1).next();
        // Bottom Right (End Color)
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(red2, green2, blue2, alpha2).next();
        // Top Right (End Color)
        bufferBuilder.vertex(matrix, x + width + skewOffset, y, 0).color(red2, green2, blue2, alpha2).next();

        tessellator.draw();
        // RenderSystem.shadeModel intentionally removed for 1.20+
        RenderSystem.disableBlend();
    }
}
