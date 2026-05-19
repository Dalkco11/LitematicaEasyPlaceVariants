package com.dalkco.litematicaeasyplacevariants.mixin;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DataManager.class, remap = false)
public class DataManagerMixin {
    @Unique
    private static ConfigGuiTab litematicaEasyPlaceVariants$originalTab = null;

    @Inject(method = "save(Z)V", at = @At("HEAD"), remap = false)
    private static void onSaveHead(boolean force, CallbackInfo ci) {
        ConfigGuiTab current = DataManager.getConfigGuiTab();
        if (current == null) {
            litematicaEasyPlaceVariants$originalTab = null;
            DataManager.setConfigGuiTab(ConfigGuiTab.GENERIC);
        } else {
            litematicaEasyPlaceVariants$originalTab = current;
        }
    }

    @Inject(method = "save(Z)V", at = @At("RETURN"), remap = false)
    private static void onSaveReturn(boolean force, CallbackInfo ci) {
        if (litematicaEasyPlaceVariants$originalTab == null) {
            DataManager.setConfigGuiTab(null);
        }
    }
}
