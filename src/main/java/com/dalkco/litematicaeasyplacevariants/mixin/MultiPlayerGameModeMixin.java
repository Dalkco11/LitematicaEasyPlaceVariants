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
        LitematicaEasyPlaceVariantsMod.LOGGER.info("modifyUseItemOnHitResult: isHandling = {}", fi.dy.masa.litematica.util.EasyPlaceUtils.isHandling());
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
            BlockState clickedState = player.level().getBlockState(clickedPos);
            boolean isInteractive = LitematicaEasyPlaceVariantsMod.isInteractive(clickedState);
            boolean isSneaking = player.isShiftKeyDown() || fi.dy.masa.litematica.util.EasyPlaceUtils.isHandling();

            if (isInteractive && !isSneaking) {
                return hitResult;
            }

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
                    } else if (schematicState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                        BlockPos[] searchOffsets = new BlockPos[]{
                            targetPos.below(),
                            targetPos.north(),
                            targetPos.south(),
                            targetPos.west(),
                            targetPos.east(),
                            targetPos.above()
                        };
                        Direction[] clickFaces = new Direction[]{
                            Direction.UP,
                            Direction.SOUTH,
                            Direction.NORTH,
                            Direction.EAST,
                            Direction.WEST,
                            Direction.DOWN
                        };

                        // 1. First pass: look for non-interactive solid neighbor
                        for (int i = 0; i < searchOffsets.length; i++) {
                            BlockPos neighbor = searchOffsets[i];
                            BlockState neighborState = player.level().getBlockState(neighbor);
                            if (!neighborState.isAir() 
                                && !(neighborState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                                && !LitematicaEasyPlaceVariantsMod.isInteractive(neighborState)) {
                                Direction clickFace = clickFaces[i];
                                Vec3 hitVec = new Vec3(
                                    neighbor.getX() + 0.5 + clickFace.getStepX() * 0.5,
                                    neighbor.getY() + 0.5 + clickFace.getStepY() * 0.5,
                                    neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.5
                                );
                                return new BlockHitResult(hitVec, clickFace, neighbor, false);
                            }
                        }

                        // 2. Second pass: fallback to any solid neighbor (excluding chests)
                        for (int i = 0; i < searchOffsets.length; i++) {
                            BlockPos neighbor = searchOffsets[i];
                            BlockState neighborState = player.level().getBlockState(neighbor);
                            if (!neighborState.isAir() 
                                && !(neighborState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) {
                                Direction clickFace = clickFaces[i];
                                Vec3 hitVec = new Vec3(
                                    neighbor.getX() + 0.5 + clickFace.getStepX() * 0.5,
                                    neighbor.getY() + 0.5 + clickFace.getStepY() * 0.5,
                                    neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.5
                                );
                                return new BlockHitResult(hitVec, clickFace, neighbor, false);
                            }
                        }
                    } else if (schematicState.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock) {
                        net.minecraft.world.level.block.state.properties.Half half = schematicState.getValue(net.minecraft.world.level.block.TrapDoorBlock.HALF);
                        Direction facing = LitematicaEasyPlaceVariantsMod.getFacing(schematicState);

                        if (half == net.minecraft.world.level.block.state.properties.Half.BOTTOM) {
                            BlockPos blockBelow = targetPos.below();
                            if (!player.level().getBlockState(blockBelow).isAir()) {
                                Vec3 hitVec = new Vec3(
                                    targetPos.getX() + 0.5,
                                    targetPos.getY(),
                                    targetPos.getZ() + 0.5
                                );
                                return new BlockHitResult(hitVec, Direction.UP, blockBelow, false);
                            }
                        } else {
                            BlockPos blockAbove = targetPos.above();
                            if (!player.level().getBlockState(blockAbove).isAir()) {
                                Vec3 hitVec = new Vec3(
                                    targetPos.getX() + 0.5,
                                    targetPos.getY() + 1.0,
                                    targetPos.getZ() + 0.5
                                );
                                return new BlockHitResult(hitVec, Direction.DOWN, blockAbove, false);
                            }
                        }

                        if (facing != null) {
                            BlockPos neighbor = targetPos.relative(facing.getOpposite());
                            if (!player.level().getBlockState(neighbor).isAir()) {
                                Direction clickFace = facing;
                                Vec3 hitVec = new Vec3(
                                    neighbor.getX() + 0.5 + clickFace.getStepX() * 0.5,
                                    neighbor.getY() + (half == net.minecraft.world.level.block.state.properties.Half.BOTTOM ? 0.2 : 0.8),
                                    neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.5
                                );
                                return new BlockHitResult(hitVec, clickFace, neighbor, false);
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
        LitematicaEasyPlaceVariantsMod.LOGGER.info("onUseItemOnHead: isHandling = {}", fi.dy.masa.litematica.util.EasyPlaceUtils.isHandling());
        boolean variantsEnabled = LitematicaEasyPlaceVariantsMod.ENABLE_VARIANTS.getBooleanValue();
        boolean fixEnabled = LitematicaEasyPlaceVariantsMod.EASY_PLACE_FIX.getBooleanValue();

        if (variantsEnabled && fixEnabled) {
            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = player.level().getBlockState(clickedPos);
            boolean isInteractive = LitematicaEasyPlaceVariantsMod.isInteractive(clickedState);
            boolean isSneaking = player.isShiftKeyDown() || fi.dy.masa.litematica.util.EasyPlaceUtils.isHandling();

            if (isInteractive && !isSneaking) {
                return;
            }

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
                    boolean shouldSneak = LitematicaEasyPlaceVariantsMod.isInteractive(clickedState)
                        && fi.dy.masa.litematica.util.EasyPlaceUtils.isHandling();

                    LitematicaEasyPlaceVariantsMod.LOGGER.info("useItemOn [HEAD] clickedPos: {}, targetPos: {}, clickedState: {}, schematicState: {}, heldItem: {}, shouldSneak: {}, isShiftKeyDown: {}",
                        clickedPos, targetPos, clickedState, schematicState, heldItem, shouldSneak, player.isShiftKeyDown());

                    if (shouldSneak && !player.isShiftKeyDown()) {
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
        LitematicaEasyPlaceVariantsMod.LOGGER.info("useItemOn [RETURN] result: {}", cir.getReturnValue());

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
