package io.github.marcsanzdev.chestseparators.util;

import io.github.marcsanzdev.chestseparators.data.SlotWhitelist;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Protects item counts assigned to player-inventory filters from automated outbound transfers.
 * Only matching items are protected; a wrong item in a filtered slot can still be deposited.
 * A positive target is shared by all slots of one filter group, per allowed item type.
 */
public final class InventoryProtection {
    private InventoryProtection() {}

    public static int movableCount(Inventory inventory, Map<Integer, SlotWhitelist> filters, int slot) {
        if (slot < 0 || slot >= inventory.getContainerSize()) return 0;
        ItemStack source = inventory.getItem(slot);
        if (source.isEmpty()) return 0;
        if (filters == null) return source.getCount();
        SlotWhitelist filter = filters.get(slot);
        if (filter == null) return source.getCount();

        String itemId = BuiltInRegistries.ITEM.getKey(source.getItem()).toString();
        if (!filter.allowedItems().contains(itemId)) return source.getCount();
        if (filter.targetCount() <= 0) return 0;

        // Retain the target in earlier slots of this group first, then keep only the shortfall here.
        // Using the live inventory means later slots account for any changes made to earlier ones.
        int earlier = 0;
        for (int i = 0; i < slot; i++) {
            SlotWhitelist previous = filters.get(i);
            if (previous == null || !filter.groupId().equals(previous.groupId())
                    || !previous.allowedItems().contains(itemId)) continue;
            ItemStack existing = inventory.getItem(i);
            if (!existing.isEmpty() && BuiltInRegistries.ITEM.getKey(existing.getItem()).toString().equals(itemId)) {
                earlier += existing.getCount();
            }
        }
        return InventoryTransferPolicy.movableCount(source.getCount(), filter.targetCount(), earlier);
    }
}
