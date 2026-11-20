package io.github.marcsanzdev.chestseparators.util;

/** Pure, backwards-compatible mapping for the compact inventory Keep slider. */
public final class InventoryRetentionSteps {
    // Slider is intentionally reversed: unlimited on the left, one on the far right.
    private static final int[] STOPS = {0, 64, 32, 16, 1};

    private InventoryRetentionSteps() {}

    public static int count() {
        return STOPS.length;
    }

    public static int valueForStop(int index) {
        return STOPS[Math.max(0, Math.min(STOPS.length - 1, index))];
    }

    public static int stopAtFraction(double fraction) {
        double bounded = Math.max(0, Math.min(1, fraction));
        return (int) Math.round(bounded * (STOPS.length - 1));
    }

    public static int stopForSavedValue(int value) {
        for (int i = 0; i < STOPS.length; i++) if (STOPS[i] == value) return i;
        // Don't change non-standard counts from old saves just by rendering the editor.
        if (value <= 0) return 0;
        int best = 1;
        for (int i = 2; i < STOPS.length; i++) {
            if (Math.abs(STOPS[i] - value) < Math.abs(STOPS[best] - value)) best = i;
        }
        return best;
    }
}
