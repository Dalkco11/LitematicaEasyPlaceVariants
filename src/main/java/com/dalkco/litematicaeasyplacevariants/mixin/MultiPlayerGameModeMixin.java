package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), remap = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        boolean variantsEnabled = LitematicaEasyPlaceVariantsMod.ENABLE_VARIANTS.getBooleanValue();
        boolean fixEnabled = LitematicaEasyPlaceVariantsMod.EASY_PLACE_FIX.getBooleanValue();

        if (variantsEnabled && fixEnabled) {
            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos targetPos = clickedPos;

            try {
                BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hitResult));
                if (!player.level().getBlockState(clickedPos).canBeReplaced(context)) {
                    targetPos = clickedPos.relative(hitResult.getDirection());
                }
            } catch (Exception e) {
                targetPos = clickedPos.relative(hitResult.getDirection());
            }

            WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                BlockState schematicState = schematicWorld.getBlockState(targetPos);

                ItemStack heldItem = player.getItemInHand(hand);
                ItemStack reqItem = new ItemStack(schematicState.getBlock().asItem());

                boolean matchesItem = (reqItem.getItem() == heldItem.getItem())
                    || LitematicaEasyPlaceVariantsMod.isEquivalent(reqItem, heldItem);

                if (matchesItem && !heldItem.isEmpty()) {
                    float[] lookDir = LitematicaEasyPlaceVariantsMod.getRequiredLookDirection(schematicState);
                    if (lookDir != null) {
                        LitematicaEasyPlaceVariantsMod.originalYaw = player.getYRot();
                        LitematicaEasyPlaceVariantsMod.originalPitch = player.getXRot();
                        LitematicaEasyPlaceVariantsMod.isRotating = true;

                        player.setYRot(lookDir[0]);
                        player.setXRot(lookDir[1]);

                        if (player.connection != null) {
                            player.connection.send(new ServerboundMovePlayerPacket.Rot(
                                lookDir[0],
                                lookDir[1],
                                player.onGround(),
                                player.horizontalCollision
                            ));
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"), remap = true)
    private void onUseItemOnReturn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (LitematicaEasyPlaceVariantsMod.isRotating) {
            float origYaw = LitematicaEasyPlaceVariantsMod.originalYaw;
            float origPitch = LitematicaEasyPlaceVariantsMod.originalPitch;
            LitematicaEasyPlaceVariantsMod.isRotating = false;

            player.setYRot(origYaw);
            player.setXRot(origPitch);

            if (player.connection != null) {
                player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    origYaw,
                    origPitch,
                    player.onGround(),
                    player.horizontalCollision
                ));
            }
        }
    }
}
