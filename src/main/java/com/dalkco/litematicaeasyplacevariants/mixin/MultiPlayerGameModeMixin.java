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
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @ModifyVariable(method = "useItemOn", at = @At("HEAD"), argsOnly = true, remap = true)
    private BlockHitResult modifyUseItemOnHitResult(BlockHitResult hitResult) {
        LitematicaEasyPlaceVariantsMod.isHopperRedirect = false;
        boolean variantsEnabled = LitematicaEasyPlaceVariantsMod.ENABLE_VARIANTS.getBooleanValue();
        boolean fixEnabled = LitematicaEasyPlaceVariantsMod.EASY_PLACE_FIX.getBooleanValue();

        if (variantsEnabled && fixEnabled) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) {
                return hitResult;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos targetPos = clickedPos;

            try {
                BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
                if (!player.level().getBlockState(clickedPos).canBeReplaced(context)) {
                    targetPos = clickedPos.relative(hitResult.getDirection());
                }
            } catch (Exception e) {
                targetPos = clickedPos.relative(hitResult.getDirection());
            }

            WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                BlockState schematicState = schematicWorld.getBlockState(targetPos);

                InteractionHand hand = null;
                ItemStack reqItem = new ItemStack(schematicState.getBlock().asItem());
                if (LitematicaEasyPlaceVariantsMod.isEquivalent(reqItem, player.getItemInHand(InteractionHand.MAIN_HAND))) {
                    hand = InteractionHand.MAIN_HAND;
                } else if (LitematicaEasyPlaceVariantsMod.isEquivalent(reqItem, player.getItemInHand(InteractionHand.OFF_HAND))) {
                    hand = InteractionHand.OFF_HAND;
                }

                if (hand != null) {
                    if (schematicState.getBlock() instanceof net.minecraft.world.level.block.HopperBlock) {
                        Direction facing = LitematicaEasyPlaceVariantsMod.getFacing(schematicState);
                        if (facing != null) {
                            if (facing == Direction.DOWN) {
                                BlockPos blockBelow = targetPos.below();
                                Vec3 hitVec = new Vec3(
                                    targetPos.getX() + 0.5,
                                    targetPos.getY(),      // y = bottom of targetPos = top of blockBelow
                                    targetPos.getZ() + 0.5
                                );
                                BlockHitResult customHit = new BlockHitResult(hitVec, Direction.UP, blockBelow, false);
                                LitematicaEasyPlaceVariantsMod.isHopperRedirect = true;
                                return customHit;
                            } else if (facing.getAxis().isHorizontal()) {
                                BlockPos neighbor = targetPos.relative(facing);
                                if (!player.level().getBlockState(neighbor).isAir()) {
                                    Direction clickFace = facing.getOpposite();
                                    Vec3 hitVec = new Vec3(
                                        neighbor.getX() + 0.5 + clickFace.getStepX() * 0.5,
                                        neighbor.getY() + 0.5 + clickFace.getStepY() * 0.5,
                                        neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.5
                                    );
                                    BlockHitResult customHit = new BlockHitResult(hitVec, clickFace, neighbor, false);
                                    LitematicaEasyPlaceVariantsMod.isHopperRedirect = true;
                                    return customHit;
                                }
                            }
                        }
                    }
                }
            }
        }
        return hitResult;
    }

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

                boolean isHopper = schematicState.getBlock() instanceof net.minecraft.world.level.block.HopperBlock;
                if (matchesItem && !heldItem.isEmpty()) {
                    if (!player.isShiftKeyDown()) {
                        LitematicaEasyPlaceVariantsMod.isForcingSneak = true;
                        player.setShiftKeyDown(true);
                        LitematicaEasyPlaceVariantsMod.sendFakeSneakPacket(player, true);
                    }

                    if (!isHopper) {
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
    }

    @Inject(method = "useItemOn", at = @At("RETURN"), remap = true)
    private void onUseItemOnReturn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (LitematicaEasyPlaceVariantsMod.isForcingSneak) {
            LitematicaEasyPlaceVariantsMod.isForcingSneak = false;
            player.setShiftKeyDown(false);
            LitematicaEasyPlaceVariantsMod.sendFakeSneakPacket(player, false);
        }

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
