package io.github.marcsanzdev.chestseparators.mixin;

import io.github.marcsanzdev.chestseparators.ChestSeparatorsState;
import io.github.marcsanzdev.chestseparators.data.SlotWhitelist;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import io.github.marcsanzdev.chestseparators.util.InventorySlotRefill;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When shift-clicking armor whose target armor slot is blocked by a Shift filter, vanilla's
 * {@code quickMoveStack} returns without a fallback — the item stays put. This adds a fallback that
 * inserts the armor into the main inventory or hotbar instead (like shift-clicking any non-armor item).
 *
 * <p>InventoryMenu slot layout: 0=craft output, 1-4=craft grid, 5-8=armor, 9-35=main, 36-44=hotbar,
 * 45=offhand. {@code moveItemStackTo} is reached via {@link AbstractContainerMenuAccessor}.
 */
@Mixin(InventoryMenu.class)
public abstract class PlayerScreenHandlerFallbackMixin {

    /**
     * Vanilla InventoryMenu.quickMoveStack sends main-inventory items straight to the hotbar,
     * never consulting dedicated MAIN filters. Give matching filtered main slots first refusal.
     * Run on BOTH logical sides to keep predicted clicks aligned with the server.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void chestseparators$preferFilteredMainOnShift(
            Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (player == null) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) return;
        Slot sourceSlot = menu.slots.get(slotIndex);
        if (!(sourceSlot.container instanceof Inventory)) return;
        int raw = sourceSlot.getContainerSlot();
        if (raw < 9 || raw >= Inventory.INVENTORY_SIZE || !sourceSlot.hasItem()) return;

        Map<Integer, SlotWhitelist> filters = ChestSeparatorsState.INVENTORY_FILTERS.get(player.getUUID());
        if (filters == null || filters.isEmpty() || filters.containsKey(raw)) return;
        ItemStack source = sourceSlot.getItem();
        String id = BuiltInRegistries.ITEM.getKey(source.getItem()).toString();
        boolean dedicated = filters.entrySet().stream().anyMatch(e ->
                e.getKey() >= 9 && e.getKey() < Inventory.INVENTORY_SIZE
                        && e.getValue().allowedItems().contains(id));
        if (!dedicated) return;

        ItemStack original = source.copy();
        boolean changed = InventorySlotRefill.refill(player.getInventory(), filters);
        if (!changed) return;
        if (!player.level().isClientSide()) menu.broadcastChanges();
        // If all items moved into dedicated main slots, suppress vanilla's hotbar fallback.
        // Otherwise vanilla can shift-move the surplus in the source slot as usual.
        if (player.getInventory().getItem(raw).isEmpty()) cir.setReturnValue(original);
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void chestseparators$armorFallback(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (slotIndex < 0 || slotIndex >= self.slots.size()) return;
        Slot slot = self.slots.get(slotIndex);
        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) return;

        var equippable = slotStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;

        int rawIndex =
                switch (equippable.slot()) {
                    case FEET -> 36;
                    case LEGS -> 37;
                    case CHEST -> 38;
                    case HEAD -> 39;
                    default -> -1;
                };
        if (rawIndex == -1) return;

        Map<Integer, SlotWhitelist> filters = ChestSeparatorsState.INVENTORY_FILTERS.get(player.getUUID());
        if (filters == null) return;
        SlotWhitelist wl = filters.get(rawIndex);
        if (wl == null || !wl.allowShift()) return;
        String itemId = BuiltInRegistries.ITEM.getKey(slotStack.getItem()).toString();
        if (wl.allowedItems().contains(itemId)) return;

        ItemStack copy = slotStack.copy();
        boolean fromHotbar = slotIndex >= 36 && slotIndex < 45;
        AbstractContainerMenuAccessor acc = (AbstractContainerMenuAccessor) (Object) this;
        if (fromHotbar) {
            acc.chestseparators$moveItemStackTo(slotStack, 9, 36, false);
        } else {
            if (!acc.chestseparators$moveItemStackTo(slotStack, 36, 45, false)) {
                acc.chestseparators$moveItemStackTo(slotStack, 9, 36, false);
            }
        }

        if (slotStack.getCount() != copy.getCount()) {
            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            slot.setChanged();
            cir.setReturnValue(copy);
        }
    }
}
