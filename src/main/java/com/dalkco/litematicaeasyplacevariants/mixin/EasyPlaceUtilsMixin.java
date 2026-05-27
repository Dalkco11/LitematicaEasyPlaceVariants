package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EasyPlaceUtils.class, remap = false)
public class EasyPlaceUtilsMixin {

    @Inject(method = "canPlaceBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCanPlaceBlock(BlockPos pos, Level world, BlockState schematicState, BlockState worldState, CallbackInfoReturnable<Boolean> cir) {
        if (LitematicaEasyPlaceVariantsMod.areStatesEquivalent(schematicState, worldState)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleEasyPlace", at = @At("RETURN"), cancellable = true, remap = false)
    private static void onHandleEasyPlaceReturn(CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.FAIL) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                BlockPos pos = blockHitResult.getBlockPos();
                if (mc.level != null && mc.player != null && !mc.player.isShiftKeyDown()) {
                    BlockState worldState = mc.level.getBlockState(pos);
                    WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
                    if (schematicWorld != null) {
                        BlockState schematicState = schematicWorld.getBlockState(pos);
                        if (LitematicaEasyPlaceVariantsMod.areBlocksEquivalent(schematicState, worldState)
                            && LitematicaEasyPlaceVariantsMod.isInteractive(worldState)) {
                            cir.setReturnValue(InteractionResult.PASS);
                        }
                    }
                }
            }
        }
    }
}

