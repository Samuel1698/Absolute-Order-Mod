package io.github.marcsanzdev.chestseparators.util;

import io.github.marcsanzdev.chestseparators.data.SlotWhitelist;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Rehomes loose items in the main inventory into matching filtered main-inventory slots.
 * The hotbar (0-8), offhand, armor, and all other reserved source slots are deliberately untouched.
 * Only an unfiltered MAIN source can be consumed; a full home slot leaves surplus where it is.
 * This is separate from retention quotas: fill to the item's stack limit first, then the
 * subsequent transfer may move the portion exceeding the group's configured Keep count.
 */
public final class InventorySlotRefill {
    private InventorySlotRefill() {}

    public static boolean refill(Inventory inventory, Map<Integer, SlotWhitelist> filters) {
        if (filters == null || filters.isEmpty()) return false;
        boolean changed = false;
        // Pass 1 completes existing filtered stacks before using vacant filtered slots.
        for (int target = 9; target < Inventory.INVENTORY_SIZE; target++) {
            SlotWhitelist filter = filters.get(target);
            if (filter == null || filter.allowedItems().isEmpty()) continue;
            ItemStack home = inventory.getItem(target);
            if (home.isEmpty() || !filter.allowedItems().contains(itemId(home))) continue;
            changed |= topUp(inventory, filters, target, home);
        }
        // Pass 2 fills empty dedicated slots according to the filter's ordered item preferences.
        for (int target = 9; target < Inventory.INVENTORY_SIZE; target++) {
            SlotWhitelist filter = filters.get(target);
            if (filter == null || filter.allowedItems().isEmpty() || !inventory.getItem(target).isEmpty()) continue;
            for (String wanted : filter.allowedItems()) {
                int source = findSource(inventory, filters, target, wanted, null);
                if (source < 0) continue;
                ItemStack sourceStack = inventory.getItem(source);
                inventory.setItem(target, sourceStack.copy());
                inventory.setItem(source, ItemStack.EMPTY);
                changed = true;
                changed |= topUp(inventory, filters, target, inventory.getItem(target));
                break;
            }
        }
        if (changed) inventory.setChanged();
        return changed;
    }

    private static boolean topUp(Inventory inventory, Map<Integer, SlotWhitelist> filters,
            int target, ItemStack home) {
        boolean changed = false;
        String itemId = itemId(home);
        int limit = Math.min(home.getMaxStackSize(), inventory.getMaxStackSize(home));
        while (home.getCount() < limit) {
            int source = findSource(inventory, filters, target, itemId, home);
            if (source < 0) break;
            ItemStack extra = inventory.getItem(source);
            int take = Math.min(limit - home.getCount(), extra.getCount());
            if (take <= 0) break;
            home.grow(take);
            extra.shrink(take);
            inventory.setItem(source, extra.isEmpty() ? ItemStack.EMPTY : extra);
            inventory.setItem(target, home);
            changed = true;
        }
        return changed;
    }

    private static int findSource(Inventory inventory, Map<Integer, SlotWhitelist> filters,
            int target, String itemId, ItemStack matching) {
        for (int slot = 9; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (slot == target || filters.containsKey(slot)) continue;
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty() || !itemId(candidate).equals(itemId)) continue;
            if (matching != null && !ItemStack.isSameItemSameComponents(matching, candidate)) continue;
            return slot;
        }
        return -1;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
