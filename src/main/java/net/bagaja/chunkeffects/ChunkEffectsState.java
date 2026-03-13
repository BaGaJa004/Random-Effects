package net.bagaja.chunkeffects;

/**
 * Shared client-side state for ChunkEffects.
 * Mirrors the ModState pattern used in the reference mod.
 */
public class ChunkEffectsState {

    /** Whether the mod is active at all. */
    public static boolean isModEnabled = true;

    /** Which mode is currently active. */
    public static ChunkEffectsMode currentMode = ChunkEffectsMode.CHUNK_CHAOS;

    /**
     * Cycle length for Timed-Chaos mode in ticks.
     * Defaults to 5 minutes (6000 ticks). Configurable via the UI.
     */
    public static int cycleTicks = 6000;

    // ---------------------------------------------------------------
    // Timed-mode state (server-authoritative, synced to client)
    // ---------------------------------------------------------------

    /**
     * How many ticks remain in the current effect window.
     * 0 means we are in the "rest" period between effects.
     */
    public static int timedEffectTicksRemaining = 0;

    /**
     * How many ticks remain in the current rest period.
     * 0 means an effect is active (or we just started).
     */
    public static int timedRestTicksRemaining = 0;

    private ChunkEffectsState() {}
}