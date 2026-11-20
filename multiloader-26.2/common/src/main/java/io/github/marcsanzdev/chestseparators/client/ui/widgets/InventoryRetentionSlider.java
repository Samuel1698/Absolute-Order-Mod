package io.github.marcsanzdev.chestseparators.client.ui.widgets;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Five discrete retention stops, left to right: All, 64, 32, 16, 1. */
public final class InventoryRetentionSlider extends CustomWidget {
    private final IntSupplier current;
    private final IntConsumer update;
    private boolean dragging;

    public InventoryRetentionSlider(int x, int y, int width, int height,
            IntSupplier current, IntConsumer update) {
        super(x, y, width, height, null);
        this.current = current;
        this.update = update;
        this.tooltipText = "Retain this many per item in the filter group: All / 64 / 32 / 16 / 1";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isDisabled || !isHovering(mouseX, mouseY)) return false;
        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    public boolean mouseDragged(double mouseX, int button) {
        if (!dragging || button != 0) return false;
        updateFromMouse(mouseX);
        return true;
    }

    public void stopDrag() {
        dragging = false;
    }

    private int trackStart() {
        return x + 57;
    }

    private int trackEnd() {
        return x + width - 7;
    }

    private void updateFromMouse(double mouseX) {
        int span = Math.max(1, trackEnd() - trackStart());
        double fraction = Math.max(0, Math.min(1, (mouseX - trackStart()) / span));
        int index = io.github.marcsanzdev.chestseparators.util.InventoryRetentionSteps.stopAtFraction(fraction);
        update.accept(io.github.marcsanzdev.chestseparators.util.InventoryRetentionSteps.valueForStop(index));
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (isDisabled) return;
        int value = current.getAsInt();
        boolean hover = isHovering(mouseX, mouseY);
        io.github.marcsanzdev.chestseparators.client.ui.UiTheme.button(
                context, x, y, width, height, hover, false);
        String label = value <= 0 ? "Keep: All" : "Keep: " + value;
        context.pose().pushMatrix();
        context.pose().translate(x + 3.0f, y + 2.0f);
        context.pose().scale(0.68f, 0.68f);
        context.text(Minecraft.getInstance().font, label, 0, 0,
                io.github.marcsanzdev.chestseparators.client.ui.UiTheme.TEXT, false);
        context.pose().popMatrix();

        int start = trackStart();
        int end = trackEnd();
        int mid = y + height / 2;
        context.fill(start, mid - 1, end + 1, mid + 1, 0xFF626262);
        int stops = io.github.marcsanzdev.chestseparators.util.InventoryRetentionSteps.count();
        for (int i = 0; i < stops; i++) {
            int tickX = start + (end - start) * i / (stops - 1);
            context.fill(tickX, mid - 3, tickX + 1, mid + 4, 0xFF989898);
        }
        int knob = start + (end - start)
                * io.github.marcsanzdev.chestseparators.util.InventoryRetentionSteps.stopForSavedValue(value)
                / (stops - 1);
        context.fill(knob - 2, mid - 4, knob + 3, mid + 5, 0xFFDEDEDE);
        if (hover && tooltipText != null) {
            context.setTooltipForNextFrame(Minecraft.getInstance().font,
                    Component.literal(tooltipText), mouseX, mouseY);
        }
    }
}
