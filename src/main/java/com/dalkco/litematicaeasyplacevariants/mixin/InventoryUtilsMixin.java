package com.dalkco.litematicaeasyplacevariants.mixin;

import com.dalkco.litematicaeasyplacevariants.LitematicaEasyPlaceVariantsMod;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InventoryUtils.class, remap = false)
public class InventoryUtilsMixin {

    @Inject(method = "schematicWorldPickBlock", at = @At("HEAD"), cancellable = true)
    private static void onSchematicWorldPickBlock(ItemStack stack, BlockPos pos, Level world, Minecraft mc, CallbackInfo ci) {
        LocalPlayer player = mc.player;
        if (player != null && !player.isCreative()) {
            Inventory inventory = player.getInventory();
            if (inventory.findSlotMatchingItem(stack) != -1) {
                return;
            }

            ItemStack variantStack = ItemStack.EMPTY;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (!itemStack.isEmpty() && LitematicaEasyPlaceVariantsMod.isEquivalent(stack, itemStack)) {
                    variantStack = itemStack;
                    break;
                }
            }

            if (variantStack.isEmpty() && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue()) {
                AbstractContainerMenu menu = player.inventoryMenu;
                for (int i = 0; i < menu.slots.size(); i++) {
                    Slot slot = menu.slots.get(i);
                    if (fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.index, false)) {
                        ItemStack boxStack = slot.getItem();
                        Block block = Block.byItem(boxStack.getItem());
                        if (block != null && block.defaultBlockState().is(BlockTags.SHULKER_BOXES)) {
                            NonNullList<ItemStack> boxItems = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(boxStack);
                            for (ItemStack innerStack : boxItems) {
                                if (!innerStack.isEmpty() && LitematicaEasyPlaceVariantsMod.isEquivalent(stack, innerStack)) {
                                    variantStack = boxStack;
                                    break;
                                }
                            }
                        }
                    }
                    if (!variantStack.isEmpty()) {
                        break;
                    }
                }
            }

            if (!variantStack.isEmpty()) {
                InventoryUtils.schematicWorldPickBlock(variantStack, pos, world, mc);
                ci.cancel();
            }
        }
    }

    @Inject(method = "getPickedItemHandSlotNoSwap(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/Minecraft;)I", at = @At("HEAD"), cancellable = true)
    private static void onGetPickedItemHandSlotNoSwap(ItemStack stack, Minecraft mc, CallbackInfoReturnable<Integer> cir) {
        LocalPlayer player = mc.player;
        if (player != null) {
            Inventory inventory = player.getInventory();
            int exactSlot = inventory.findSlotMatchingItem(stack);
            if (exactSlot != -1) {
                return;
            }

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (!itemStack.isEmpty() && LitematicaEasyPlaceVariantsMod.isEquivalent(stack, itemStack)) {
                    cir.setReturnValue(InventoryUtils.getPickedItemHandSlotNoSwap(i, itemStack, mc));
                    return;
                }
            }
        }
    }
}
