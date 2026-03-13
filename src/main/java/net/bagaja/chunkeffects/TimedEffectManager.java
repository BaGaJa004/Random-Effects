package net.bagaja.chunkeffects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the Timed-Chaos mode:
 *
 *   [ effect active for effectDuration ticks ]
 *   [ rest period: CYCLE_TICKS - effectDuration ticks ]
 *   [ next effect ... ]
 *
 * Total cycle length is always CYCLE_TICKS (5 minutes = 6000 ticks).
 * The effect duration is a random slice of that cycle:
 *   between MIN_EFFECT_TICKS (5 s) and MAX_EFFECT_TICKS (90 s).
 * The rest fills the remaining time.
 *
 * Per-player state is kept server-side.  The class is package-private and
 * called from ChunkEffectsMod's tick handler.
 */
public class TimedEffectManager {

    // 5 minutes in ticks
    public static final int CYCLE_TICKS = 6000;

    // Effect lasts between 5 seconds and 90 seconds
    private static final int MIN_EFFECT_TICKS = 100;   // 5 s
    private static final int MAX_EFFECT_TICKS = 1800;  // 90 s

    private static final int MAX_AMPLIFIER = 4;
    private static final Random RANDOM = new Random();

    // ---------------------------------------------------------------
    // Per-player state
    // ---------------------------------------------------------------

    private MobEffect currentEffect   = null;
    private int       currentAmplifier = 0;

    /**
     * Ticks remaining in the current effect window (>0 → effect is active).
     */
    private int effectTicksLeft = 0;

    /**
     * Ticks remaining in the rest window (>0 → resting, no effect).
     * When this also hits 0, a new cycle begins.
     */
    private int restTicksLeft   = 0;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Call once per server tick for the owning player.
     * Applies or removes the mob effect as appropriate.
     */
    public void tick(Player player) {
        if (effectTicksLeft > 0) {
            // --- Effect phase ---
            applyEffect(player);
            effectTicksLeft--;

            if (effectTicksLeft == 0) {
                // Effect just ended — remove it immediately and start rest
                player.removeEffect(currentEffect);
                // restTicksLeft was already set when the cycle started
            }

        } else if (restTicksLeft > 0) {
            // --- Rest phase ---
            restTicksLeft--;

            if (restTicksLeft == 0) {
                // Rest over → start a new cycle
                startNewCycle(player);
            }

        } else {
            // First tick ever (or mod was just enabled)
            startNewCycle(player);
        }
    }

    /** Call when the mode changes away from TIMED_CHAOS to clean up. */
    public void reset(Player player) {
        if (currentEffect != null) {
            player.removeEffect(currentEffect);
        }
        effectTicksLeft = 0;
        restTicksLeft   = 0;
        currentEffect   = null;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private void startNewCycle(Player player) {
        // Pick a random effect
        List<MobEffect> allEffects = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.forEach(allEffects::add);
        currentEffect    = allEffects.get(RANDOM.nextInt(allEffects.size()));
        currentAmplifier = RANDOM.nextInt(MAX_AMPLIFIER + 1);

        // Pick a random effect duration within the allowed range
        int range        = MAX_EFFECT_TICKS - MIN_EFFECT_TICKS;
        effectTicksLeft  = MIN_EFFECT_TICKS + RANDOM.nextInt(range + 1);

        // The rest fills the remainder of the 5-minute cycle
        restTicksLeft    = CYCLE_TICKS - effectTicksLeft;

        applyEffect(player);
        effectTicksLeft--; // consume this tick
    }

    /**
     * Re-applies the effect each tick using the actual remaining ticks as the
     * duration, so the inventory UI always shows the real countdown.
     *
     * Minecraft's addEffect only updates duration when the incoming value is
     * >= the current one, so passing effectTicksLeft directly works correctly —
     * it counts down naturally between ticks and we overwrite it each tick with
     * the authoritative remaining time.
     */
    private void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(
                currentEffect,
                effectTicksLeft,  // real remaining time → inventory shows true countdown
                currentAmplifier,
                false,            // ambient (no particles)
                false,            // show particles
                true              // show icon
        ));
    }
}