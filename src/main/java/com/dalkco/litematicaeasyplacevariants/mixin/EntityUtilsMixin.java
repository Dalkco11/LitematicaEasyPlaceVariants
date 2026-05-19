package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.util.EntityUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityUtils.class, remap = false)
public class EntityUtilsMixin {

    @Inject(method = "getUsedHandForItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/InteractionHand;", at = @At("HEAD"), cancellable = true)
    private static void onGetUsedHandForItemPlayer(Player player, ItemStack stack, CallbackInfoReturnable<InteractionHand> cir) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        if (LitematicaEasyPlaceVariantsMod.isEquivalent(stack, player.getMainHandItem())) {
            cir.setReturnValue(InteractionHand.MAIN_HAND);
            return;
        }

        if (LitematicaEasyPlaceVariantsMod.isEquivalent(stack, player.getOffhandItem())) {
            cir.setReturnValue(InteractionHand.OFF_HAND);
            return;
        }
    }

    @Inject(method = "getUsedHandForItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/InteractionHand;", at = @At("HEAD"), cancellable = true)
    private static void onGetUsedHandForItemLiving(LivingEntity entity, ItemStack stack, boolean checkOffhand, CallbackInfoReturnable<InteractionHand> cir) {
        if (entity == null || stack == null || stack.isEmpty()) {
            return;
        }

        if (LitematicaEasyPlaceVariantsMod.isEquivalent(stack, entity.getItemInHand(InteractionHand.MAIN_HAND))) {
            cir.setReturnValue(InteractionHand.MAIN_HAND);
            return;
        }

        if (LitematicaEasyPlaceVariantsMod.isEquivalent(stack, entity.getItemInHand(InteractionHand.OFF_HAND))) {
            cir.setReturnValue(InteractionHand.OFF_HAND);
            return;
        }
    }
}
