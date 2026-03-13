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
 */
public class ChunkEffectsScreen extends Screen {

    // Temp state held while the menu is open (committed on Save & Close)
    private boolean          tempEnabled;
    private ChunkEffectsMode tempMode;

    public ChunkEffectsScreen() {
        super(Component.literal("Chunk Effects Manager"));
        this.tempEnabled = ChunkEffectsState.isModEnabled;
        this.tempMode    = ChunkEffectsState.currentMode;
    }

    @Override
    protected void init() {
        int centerX  = this.width  / 2;
        int centerY  = this.height / 2;
        int btnWidth  = 200;
        int btnHeight = 20;
        int padding   = 24;

        // 1. Toggle Enabled / Disabled
        this.addRenderableWidget(Button.builder(
                        Component.literal("Mod Status: " + enabledLabel(tempEnabled)),
                        button -> {
                            tempEnabled = !tempEnabled;
                            button.setMessage(Component.literal("Mod Status: " + enabledLabel(tempEnabled)));
                        })
                .bounds(centerX - btnWidth / 2, centerY - 60, btnWidth, btnHeight)
                .build());

        // 2. Toggle Mode
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

        // 3. Info label row (non-interactive — rendered as a disabled button for visual consistency)
        this.addRenderableWidget(Button.builder(
                        Component.literal(modeDescription(tempMode)),
                        button -> { /* no-op info label */ })
                .bounds(centerX - btnWidth / 2, centerY - 60 + (padding * 2), btnWidth, btnHeight)
                .build());

        // 4. Save & Close
        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
                    saveChanges();
                    this.onClose();
                })
                .bounds(centerX - btnWidth / 2, centerY + 60, btnWidth, btnHeight)
                .build());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String enabledLabel(boolean enabled) {
        return enabled ? "ENABLED" : "DISABLED";
    }

    private static String modeLabel(ChunkEffectsMode mode) {
        return switch (mode) {
            case CHUNK_CHAOS -> "CHUNK CHAOS (per-chunk)";
            case TIMED_CHAOS -> "TIMED CHAOS (5 min cycle)";
        };
    }

    private static String modeDescription(ChunkEffectsMode mode) {
        return switch (mode) {
            case CHUNK_CHAOS -> "Each chunk has its own random effect";
            case TIMED_CHAOS -> "Random effect every 5 min, random duration";
        };
    }

    private void saveChanges() {
        ChunkEffectsState.isModEnabled = tempEnabled;
        ChunkEffectsState.currentMode  = tempMode;
        // The server-side tick handler reads ChunkEffectsState directly,
        // so no packet is needed for a single-player / integrated-server setup.
        // If you add multiplayer support later, send a sync packet here.
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
