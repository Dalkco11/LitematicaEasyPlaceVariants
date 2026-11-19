package com.dalkco.litematicaeasyplacevariants;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen (opened from Mod Menu). Edits the same malilib options as the
 * "Variant Settings" tab in Litematica's config screen, so both stay in sync.
 */
public class LitematicaEasyPlaceVariantsConfigScreen extends Screen {
    private static final int W = 220;
    private static final int H = 20;
    private static final int GAP = 24;

    private final Screen parent;

    public LitematicaEasyPlaceVariantsConfigScreen(Screen parent) {
        super(Component.translatable(LitematicaEasyPlaceVariantsMod.MOD_ID + ".config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - W / 2;
        int y = Math.max(32, this.height / 6);

        for (ConfigBoolean option : LitematicaEasyPlaceVariantsMod.OPTIONS) {
            Tooltip tooltip = Tooltip.create(Component.translatable(LitematicaEasyPlaceVariantsMod.commentKey(option)));
            addRenderableWidget(CycleButton.onOffBuilder(option.getBooleanValue())
                    .withTooltip(v -> tooltip)
                    .create(x, y, W, H, Component.translatable(LitematicaEasyPlaceVariantsMod.nameKey(option)),
                            (b, v) -> {
                                option.setBooleanValue(v);
                                LitematicaEasyPlaceVariantsMod.saveConfig();
                            }));
            y += GAP;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, Math.max(y + 12, this.height - 32), W, H).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        LitematicaEasyPlaceVariantsMod.saveConfig();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}
