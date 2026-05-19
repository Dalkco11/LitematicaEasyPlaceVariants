package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Collections;

@Mixin(value = GuiConfigs.class, remap = false)
public abstract class GuiConfigsMixin {

    @SuppressWarnings("unchecked")
    private List<ButtonBase> getButtonsList() {
        try {
            java.lang.reflect.Field field = fi.dy.masa.malilib.gui.GuiBase.class.getDeclaredField("buttons");
            field.setAccessible(true);
            return (List<ButtonBase>) field.get(this);
        } catch (Exception e) {
            LitematicaEasyPlaceVariantsMod.LOGGER.error("Failed to get buttons list via reflection", e);
            return Collections.emptyList();
        }
    }

    private void callAddButton(ButtonBase button, IButtonActionListener listener) {
        try {
            java.lang.reflect.Method method = fi.dy.masa.malilib.gui.GuiBase.class.getDeclaredMethod(
                "addButton", 
                fi.dy.masa.malilib.gui.button.ButtonBase.class, 
                fi.dy.masa.malilib.gui.button.IButtonActionListener.class
            );
            method.setAccessible(true);
            method.invoke(this, button, listener);
        } catch (Exception e) {
            LitematicaEasyPlaceVariantsMod.LOGGER.error("Failed to call addButton via reflection", e);
        }
    }

    private void callReCreateListWidget() {
        try {
            java.lang.reflect.Method method = fi.dy.masa.malilib.gui.GuiListBase.class.getDeclaredMethod("reCreateListWidget");
            method.setAccessible(true);
            method.invoke(this);
        } catch (Exception e) {
            LitematicaEasyPlaceVariantsMod.LOGGER.error("Failed to call reCreateListWidget via reflection", e);
        }
    }

    private void callInitGui() {
        try {
            java.lang.reflect.Method method = fi.dy.masa.malilib.gui.GuiBase.class.getDeclaredMethod("initGui");
            method.setAccessible(true);
            method.invoke(this);
        } catch (Exception e) {
            LitematicaEasyPlaceVariantsMod.LOGGER.error("Failed to call initGui via reflection", e);
        }
    }

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void onInitGui(CallbackInfo ci) {
        if (DataManager.getConfigGuiTab() != null) {
            LitematicaEasyPlaceVariantsMod.isCustomTabActive = false;
        }

        int x = 10;
        int y = 26;

        List<ButtonBase> buttons = getButtonsList();

        if (buttons != null && !buttons.isEmpty()) {
            ButtonBase lastButton = buttons.get(buttons.size() - 1);
            x = lastButton.getX() + lastButton.getWidth() + 2;
        }

        ButtonGeneric variantButton = new ButtonGeneric(x, y, -1, 20, "Настройки Вариантов", new String[0]);
        variantButton.setEnabled(!LitematicaEasyPlaceVariantsMod.isCustomTabActive);

        this.callAddButton(variantButton, (b, buttonId) -> {
            if (buttons != null) {
                for (ButtonBase btn : buttons) {
                    if (btn instanceof ButtonGeneric && btn != b) {
                        btn.setEnabled(true);
                    }
                }
            }
            b.setEnabled(false);
            LitematicaEasyPlaceVariantsMod.isCustomTabActive = true;
            DataManager.setConfigGuiTab(null);
            this.callReCreateListWidget();
            this.callInitGui();
        });
    }

    @Inject(method = "getConfigs", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetConfigs(CallbackInfoReturnable<List<ConfigOptionWrapper>> cir) {
        if (DataManager.getConfigGuiTab() != null) {
            LitematicaEasyPlaceVariantsMod.isCustomTabActive = false;
        }

        if (LitematicaEasyPlaceVariantsMod.isCustomTabActive) {
            cir.setReturnValue(ConfigOptionWrapper.createFor(LitematicaEasyPlaceVariantsMod.OPTIONS));
        }
    }

    @Inject(method = "onSettingsChanged", at = @At("RETURN"), remap = false)
    private void onOnSettingsChanged(CallbackInfo ci) {
        LitematicaEasyPlaceVariantsMod.saveConfig();
    }
}
