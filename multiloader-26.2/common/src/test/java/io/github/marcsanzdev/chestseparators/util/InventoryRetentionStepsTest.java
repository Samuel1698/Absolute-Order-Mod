package io.github.marcsanzdev.chestseparators.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class InventoryRetentionStepsTest {
    @Test void stopOrderIsAllThen64Then32Then16Then1() {
        assertEquals(5, InventoryRetentionSteps.count());
        assertEquals(0, InventoryRetentionSteps.valueForStop(0));
        assertEquals(64, InventoryRetentionSteps.valueForStop(1));
        assertEquals(32, InventoryRetentionSteps.valueForStop(2));
        assertEquals(16, InventoryRetentionSteps.valueForStop(3));
        assertEquals(1, InventoryRetentionSteps.valueForStop(4));
    }

    @Test void slideBoundsAndBreakpoints() {
        assertEquals(0, InventoryRetentionSteps.stopAtFraction(-1));
        assertEquals(1, InventoryRetentionSteps.stopAtFraction(0.25));
        assertEquals(2, InventoryRetentionSteps.stopAtFraction(0.5));
        assertEquals(3, InventoryRetentionSteps.stopAtFraction(0.75));
        assertEquals(4, InventoryRetentionSteps.stopAtFraction(1.5));
    }

    @Test void existingCustomAmountIsNotAutomaticallyAltered() {
        assertEquals(2, InventoryRetentionSteps.stopForSavedValue(27));
        assertEquals(1, InventoryRetentionSteps.stopForSavedValue(80));
        assertEquals(0, InventoryRetentionSteps.stopForSavedValue(0));
        assertEquals(4, InventoryRetentionSteps.stopForSavedValue(2));
    }
}
