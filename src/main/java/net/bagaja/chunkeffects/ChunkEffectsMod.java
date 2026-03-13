package net.bagaja.chunkeffects;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod(ChunkEffectsMod.MODID)
public class ChunkEffectsMod {

    public static final String MODID = "chunkeffects";

    // ---------------------------------------------------------------
    // Chunk-Chaos mode state
    // ---------------------------------------------------------------
    private static final Map<Long, ChunkEffectData> chunkEffects = new HashMap<>();
    private static final Random RANDOM       = new Random();
    private static final int    MAX_AMPLIFIER = 4;

    private static class ChunkEffectData {
        final MobEffect effect;
        final int       amplifier;
        ChunkEffectData(MobEffect effect, int amplifier) {
            this.effect    = effect;
            this.amplifier = amplifier;
        }
    }

    // ---------------------------------------------------------------
    // Timed-Chaos mode state  (one manager per player UUID)
    // ---------------------------------------------------------------
    private static final Map<UUID, TimedEffectManager> timedManagers = new HashMap<>();

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public ChunkEffectsMod() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------------
    // Keyboard shortcut — press G to open the config screen (client only)
    // ---------------------------------------------------------------
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            // G key opens the config screen (change GLFW.GLFW_KEY_G to any key you prefer)
            if (event.getAction() == GLFW.GLFW_PRESS
                    && event.getKey() == GLFW.GLFW_KEY_G
                    && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new ChunkEffectsScreen());
            }
        }
    }

    // ---------------------------------------------------------------
    // Server-side tick — runs for every player every tick
    // ---------------------------------------------------------------
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;       // server side only

        if (!ChunkEffectsState.isModEnabled) {
            // Mod disabled — make sure we clean up any lingering managers
            TimedEffectManager mgr = timedManagers.get(player.getUUID());
            if (mgr != null) mgr.reset(player);
            return;
        }

        switch (ChunkEffectsState.currentMode) {
            case CHUNK_CHAOS -> tickChunkChaos(player);
            case TIMED_CHAOS -> tickTimedChaos(player);
        }
    }

    // ---------------------------------------------------------------
    // CHUNK-CHAOS: original behaviour — per-chunk random effect
    // ---------------------------------------------------------------
    private void tickChunkChaos(Player player) {
        // If the player just switched FROM timed mode, clean that up first
        TimedEffectManager mgr = timedManagers.get(player.getUUID());
        if (mgr != null) {
            mgr.reset(player);
            timedManagers.remove(player.getUUID());
        }

        long chunkPos = getChunkKey(player);

        ChunkEffectData effectData = chunkEffects.computeIfAbsent(chunkPos, k -> {
            List<MobEffect> allEffects = new ArrayList<>();
            BuiltInRegistries.MOB_EFFECT.forEach(allEffects::add);
            MobEffect randomEffect    = allEffects.get(RANDOM.nextInt(allEffects.size()));
            int       randomAmplifier = RANDOM.nextInt(MAX_AMPLIFIER + 1);
            return new ChunkEffectData(randomEffect, randomAmplifier);
        });

        player.addEffect(new MobEffectInstance(
                effectData.effect,
                40,                     // refreshed every tick
                effectData.amplifier,
                false,
                false,
                true
        ));
    }

    // ---------------------------------------------------------------
    // TIMED-CHAOS: 5-minute cycle, random effect with random duration
    // ---------------------------------------------------------------
    private void tickTimedChaos(Player player) {
        TimedEffectManager mgr = timedManagers.computeIfAbsent(
                player.getUUID(), id -> new TimedEffectManager());
        mgr.tick(player);
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private long getChunkKey(Player player) {
        int chunkX = (int) player.getX() >> 4;
        int chunkZ = (int) player.getZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}