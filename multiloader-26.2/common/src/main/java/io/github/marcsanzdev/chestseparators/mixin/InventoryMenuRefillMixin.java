package io.github.marcsanzdev.chestseparators.mixin;

import io.github.marcsanzdev.chestseparators.ChestSeparatorsState;
import io.github.marcsanzdev.chestseparators.data.SlotWhitelist;
import io.github.marcsanzdev.chestseparators.util.InventorySlotRefill;
import java.util.Map;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores matching stacks when a player deliberately moves them FROM a filtered MAIN inventory
 * slot into an unfiltered MAIN slot. Manual hotbar moves are explicitly left alone. Tracking the
 * origin instead of reacting to every inventory click avoids interrupting the Push button's own
 * multi-click transfers, including the explicit pre-deposit consolidation step.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class InventoryMenuRefillMixin {
    @Unique
    private boolean chestseparators$carryingFilteredMainStack;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void chestseparators$trackFilteredSource(
            int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (player == null) return;
        if (actionType != ContainerInput.PICKUP) {
            chestseparators$carryingFilteredMainStack = false;
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        // The first pickup begins with an empty cursor, and subsequent placements retain the origin.
        if (!menu.getCarried().isEmpty() || slotIndex < 0 || slotIndex >= menu.slots.size()) return;
        Slot source = menu.slots.get(slotIndex);
        if (!(source.container instanceof Inventory)) return;
        int raw = source.getContainerSlot();
        if (raw < 9 || raw >= Inventory.INVENTORY_SIZE || !source.hasItem()) return;
        Map<Integer, SlotWhitelist> filters = ChestSeparatorsState.INVENTORY_FILTERS.get(player.getUUID());
        // A fresh pickup from an unrelated slot must not inherit an old filtered origin.
        chestseparators$carryingFilteredMainStack = filters != null && filters.containsKey(raw);
    }

    @Inject(method = "clicked", at = @At("TAIL"))
    private void chestseparators$refillAfterDrop(
            int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (player == null
                || actionType != ContainerInput.PICKUP
                || !chestseparators$carryingFilteredMainStack) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        // Partial right-click placements should refill too, even while some items stay on the cursor.
        // Keep the origin until the cursor empties so subsequent placements of the same stack work.
        if (menu.getCarried().isEmpty()) chestseparators$carryingFilteredMainStack = false;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) return;
        Slot target = menu.slots.get(slotIndex);
        if (!(target.container instanceof Inventory)) return;
        int raw = target.getContainerSlot();
        if (raw < 9 || raw >= Inventory.INVENTORY_SIZE) return; // manual hotbar placement stays intact
        Map<Integer, SlotWhitelist> filters = ChestSeparatorsState.INVENTORY_FILTERS.get(player.getUUID());
        if (filters == null || filters.containsKey(raw)) return;
        if (InventorySlotRefill.refill(player.getInventory(), filters) && !player.level().isClientSide()) {
            menu.broadcastChanges();
        }
    }
}
