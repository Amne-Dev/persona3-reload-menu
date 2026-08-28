package com.amnedev.p3rmenu.v12111;

import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.client.gui.screens.worldselection.OptimizeWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

/** Central screen classification keeps all 26.2 menu descendants visually coherent. */
public final class P3RScreenFamily {
    private P3RScreenFamily() {
    }

    public static boolean isList(Screen screen) {
        return screen instanceof SelectWorldScreen || screen instanceof JoinMultiplayerScreen;
    }

    public static boolean isConfiguration(Screen screen) {
        return screen instanceof OptionsSubScreen
                || screen instanceof CreateWorldScreen
                || screen instanceof EditWorldScreen
                || screen instanceof EditGameRulesScreen
                || screen instanceof ExperimentsScreen
                || screen instanceof ConfirmExperimentalFeaturesScreen
                || screen instanceof OptimizeWorldScreen
                || screen instanceof CreateFlatWorldScreen
                || screen instanceof PresetFlatWorldScreen
                || screen instanceof CreateBuffetWorldScreen
                || screen instanceof PackSelectionScreen
                || screen instanceof DirectJoinServerScreen
                || screen instanceof ManageServerScreen
                || screen instanceof WarningScreen;
    }

    public static boolean isStyled(Screen screen) {
        return isList(screen) || isConfiguration(screen);
    }
}
