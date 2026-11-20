package io.github.marcsanzdev.chestseparators.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventoryRetentionStepsTest {
    @Test void marksAreAll64Then32Then16Then1() {
        assertEquals(5, InventoryRetentionSteps.count());
        assertEquals(0, InventoryRetentionSteps.valueForStop(0));
        assertEquals(64, InventoryRetentionSteps.valueForStop(1));
        assertEquals(32, InventoryRetentionSteps.valueForStop(2));
        assertEquals(16, InventoryRetentionSteps.valueForStop(3));
        assertEquals(1, InventoryRetentionSteps.valueForStop(4));
    }

    @Test void continuousMappingIncludesEveryInteger() {
        assertEquals(0, InventoryRetentionSteps.valueAtFraction(0));
        assertEquals(64, InventoryRetentionSteps.valueAtFraction(0.25));
        assertEquals(32, InventoryRetentionSteps.valueAtFraction(0.5));
        assertEquals(16, InventoryRetentionSteps.valueAtFraction(0.75));
        assertEquals(1, InventoryRetentionSteps.valueAtFraction(1));
        for (int count = 1; count <= 64; count++) {
            assertEquals(count, InventoryRetentionSteps.valueAtFraction(
                    InventoryRetentionSteps.fractionForValue(count)), "round trip " + count);
        }
    }

    @Test void savedCustomAmountsRemainUnchangedUntilEdited() {
        assertEquals(0.0, InventoryRetentionSteps.fractionForValue(0));
        assertEquals(0.25, InventoryRetentionSteps.fractionForValue(999));
        assertEquals(27, InventoryRetentionSteps.adjustByWheel(28, -1));
        assertEquals(64, InventoryRetentionSteps.adjustByWheel(0, 1));
        assertEquals(1, InventoryRetentionSteps.adjustByWheel(1, -1));
    }
}
