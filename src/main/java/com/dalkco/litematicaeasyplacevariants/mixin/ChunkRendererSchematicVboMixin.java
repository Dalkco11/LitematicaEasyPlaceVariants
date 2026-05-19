package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.util.OverlayType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
public class ChunkRendererSchematicVboMixin {

    @Inject(method = "getOverlayType", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetOverlayType(BlockState schematicState, BlockState worldState, CallbackInfoReturnable<OverlayType> cir) {
        if (LitematicaEasyPlaceVariantsMod.ENABLE_VARIANTS.getBooleanValue() && LitematicaEasyPlaceVariantsMod.VERIFY_VARIANTS_STATE.getBooleanValue()) {
            ItemStack req = new ItemStack(schematicState.getBlock().asItem());
            ItemStack cand = new ItemStack(worldState.getBlock().asItem());

            if (LitematicaEasyPlaceVariantsMod.isEquivalent(req, cand)) {
                if (LitematicaEasyPlaceVariantsMod.areStatesEquivalent(schematicState, worldState)) {
                    cir.setReturnValue(OverlayType.NONE);
                } else {
                    cir.setReturnValue(OverlayType.WRONG_STATE);
                }
            }
        }
    }
}
