package net.bagaja.chunkeffects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TimedEffectManager {

    public static final int CYCLE_TICKS = 6000;

    private static final int MIN_EFFECT_TICKS = 100;
    private static final int MAX_EFFECT_TICKS = 1800;
    private static final int MAX_AMPLIFIER = 4;
    private static final Random RANDOM = new Random();

    // Changed: MobEffect → Holder<MobEffect>
    private Holder<MobEffect> currentEffect = null;
    private int currentAmplifier = 0;
    private int effectTicksLeft = 0;
    private int restTicksLeft = 0;

    public void tick(Player player) {
        if (effectTicksLeft > 0) {
            applyEffect(player);
            effectTicksLeft--;

            if (effectTicksLeft == 0) {
                player.removeEffect(currentEffect); // now accepts Holder<MobEffect>
            }

        } else if (restTicksLeft > 0) {
            restTicksLeft--;

            if (restTicksLeft == 0) {
                startNewCycle(player);
            }

        } else {
            startNewCycle(player);
        }
    }

    public void reset(Player player) {
        if (currentEffect != null) {
            player.removeEffect(currentEffect);
        }
        effectTicksLeft = 0;
        restTicksLeft = 0;
        currentEffect = null;
    }

    private void startNewCycle(Player player) {
        // Changed: collect Holder<MobEffect> from the registry
        List<Holder.Reference<MobEffect>> allEffects = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.holders().forEach(allEffects::add);
        currentEffect = allEffects.get(RANDOM.nextInt(allEffects.size()));
        currentAmplifier = RANDOM.nextInt(MAX_AMPLIFIER + 1);

        int range = MAX_EFFECT_TICKS - MIN_EFFECT_TICKS;
        effectTicksLeft = MIN_EFFECT_TICKS + RANDOM.nextInt(range + 1);
        restTicksLeft = CYCLE_TICKS - effectTicksLeft;

        applyEffect(player);
        effectTicksLeft--;
    }

    private void applyEffect(Player player) {
        player.addEffect(new MobEffectInstance(
                currentEffect,   // Holder<MobEffect> — now correct
                effectTicksLeft,
                currentAmplifier,
                false,
                false,
                true
        ));
    }
}