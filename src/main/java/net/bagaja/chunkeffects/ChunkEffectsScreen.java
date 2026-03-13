package net.bagaja.chunkeffects;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration screen for ChunkEffects.
 *
 * Design mirrors DropConfigScreen from the reference mod:
 *  - Centered button column
 *  - Temp-state toggle pattern (changes committed only on Save & Close)
 *  - Title rendered at y=40
 *  - isPauseScreen = true
 *
 * Cycle duration uses a  ◀  [value]  ▶  selector row — the same style
 * Minecraft's own options screens use — so it looks native and is clearly
 * a multi-option picker rather than a simple toggle.
 */
public class ChunkEffectsScreen extends Screen {

    // Temp state held while the menu is open (committed on Save & Close)
    private boolean          tempEnabled;
    private ChunkEffectsMode tempMode;
    private int              tempCycleIndex; // index into CYCLE_OPTIONS

    // Available cycle durations: 1, 2, 5, 10, 15, 30 minutes in ticks
    private static final int[]    CYCLE_OPTIONS = { 1200, 2400, 6000, 12000, 18000, 36000 };
    private static final String[] CYCLE_LABELS  = { "1 min", "2 min", "5 min", "10 min", "15 min", "30 min" };

    // Kept as a field so the arrow buttons can update its label
    private Button cycleLabelBtn;

    public ChunkEffectsScreen() {
        super(Component.literal("Chunk Effects Manager"));
        this.tempEnabled    = ChunkEffectsState.isModEnabled;
        this.tempMode       = ChunkEffectsState.currentMode;
        this.tempCycleIndex = indexForTicks(ChunkEffectsState.cycleTicks);
    }

    @Override
    protected void init() {
        int centerX   = this.width  / 2;
        int centerY   = this.height / 2;
        int btnWidth  = 200;
        int btnHeight = 20;
        int padding   = 24;

        // Arrow button widths for the  ◀  [label]  ▶  row
        int arrowW = 20;
        int labelW = btnWidth - arrowW * 2; // 160 px centre label

        // ── Row 1: Mod Status toggle ──────────────────────────────────
        this.addRenderableWidget(Button.builder(
                        Component.literal("Mod Status: " + enabledLabel(tempEnabled)),
                        button -> {
                            tempEnabled = !tempEnabled;
                            button.setMessage(Component.literal("Mod Status: " + enabledLabel(tempEnabled)));
                        })
                .bounds(centerX - btnWidth / 2, centerY - 60, btnWidth, btnHeight)
                .build());

        // ── Row 2: Mode toggle ────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                        Component.literal("Mode: " + modeLabel(tempMode)),
                        button -> {
                            tempMode = (tempMode == ChunkEffectsMode.CHUNK_CHAOS)
                                    ? ChunkEffectsMode.TIMED_CHAOS
                                    : ChunkEffectsMode.CHUNK_CHAOS;
                            button.setMessage(Component.literal("Mode: " + modeLabel(tempMode)));
                        })
                .bounds(centerX - btnWidth / 2, centerY - 60 + padding, btnWidth, btnHeight)
                .build());

        // ── Row 3: Cycle duration  ◀  [value]  ▶  ────────────────────
        int rowY   = centerY - 60 + (padding * 2);
        int leftX  = centerX - btnWidth / 2;
        int labelX = leftX + arrowW;
        int rightX = labelX + labelW;

        // ◀ previous
        this.addRenderableWidget(Button.builder(
                        Component.literal("◀"),
                        button -> {
                            tempCycleIndex = (tempCycleIndex - 1 + CYCLE_OPTIONS.length) % CYCLE_OPTIONS.length;
                            cycleLabelBtn.setMessage(Component.literal(cycleDurationTitle()));
                        })
                .bounds(leftX, rowY, arrowW, btnHeight)
                .build());

        // centre label — displays the current selection, not clickable for navigation
        cycleLabelBtn = Button.builder(
                        Component.literal(cycleDurationTitle()),
                        button -> { /* display only */ })
                .bounds(labelX, rowY, labelW, btnHeight)
                .build();
        this.addRenderableWidget(cycleLabelBtn);

        // ▶ next
        this.addRenderableWidget(Button.builder(
                        Component.literal("▶"),
                        button -> {
                            tempCycleIndex = (tempCycleIndex + 1) % CYCLE_OPTIONS.length;
                            cycleLabelBtn.setMessage(Component.literal(cycleDurationTitle()));
                        })
                .bounds(rightX, rowY, arrowW, btnHeight)
                .build());

        // ── Row 4: Info label (mode description) ─────────────────────
        this.addRenderableWidget(Button.builder(
                        Component.literal(modeDescription(tempMode)),
                        button -> { /* no-op info label */ })
                .bounds(centerX - btnWidth / 2, centerY - 60 + (padding * 3), btnWidth, btnHeight)
                .build());

        // ── Row 5: Save & Close ───────────────────────────────────────
        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
                    saveChanges();
                    this.onClose();
                })
                .bounds(centerX - btnWidth / 2, centerY + 60 + padding, btnWidth, btnHeight)
                .build());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String cycleDurationTitle() {
        return "Cycle Duration: " + CYCLE_LABELS[tempCycleIndex];
    }

    private static int indexForTicks(int ticks) {
        for (int i = 0; i < CYCLE_OPTIONS.length; i++) {
            if (CYCLE_OPTIONS[i] == ticks) return i;
        }
        return 2; // default to 5 min (index 2)
    }

    private static String enabledLabel(boolean enabled) {
        return enabled ? "ENABLED" : "DISABLED";
    }

    private static String modeLabel(ChunkEffectsMode mode) {
        return switch (mode) {
            case CHUNK_CHAOS -> "CHUNK CHAOS (per-chunk)";
            case TIMED_CHAOS -> "TIMED CHAOS (timed cycle)";
        };
    }

    private static String modeDescription(ChunkEffectsMode mode) {
        return switch (mode) {
            case CHUNK_CHAOS -> "Each chunk has its own random effect";
            case TIMED_CHAOS -> "Random effect each cycle, random duration";
        };
    }

    private void saveChanges() {
        ChunkEffectsState.isModEnabled = tempEnabled;
        ChunkEffectsState.currentMode  = tempMode;
        ChunkEffectsState.cycleTicks   = CYCLE_OPTIONS[tempCycleIndex];
    }

    // ---------------------------------------------------------------
    // Screen overrides
    // ---------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}