package net.bagaja.chunkeffects;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod(ChunkEffectsMod.MODID)
public class ChunkEffectsMod {

    public static final String MODID = "chunkeffects";

    private static final Map<Long, ChunkEffectData> chunkEffects = new HashMap<>();
    private static final Random RANDOM       = new Random();
    private static final int    MAX_AMPLIFIER = 4;

    private static class ChunkEffectData {
        final Holder<MobEffect> effect;
        final int               amplifier;
        ChunkEffectData(Holder<MobEffect> effect, int amplifier) {
            this.effect    = effect;
            this.amplifier = amplifier;
        }
    }

    private static final Map<UUID, TimedEffectManager> timedManagers = new HashMap<>();

    // Delete both @Mod.EventBusSubscriber inner classes entirely,
// and put this in your constructor instead:

    public ChunkEffectsMod(FMLJavaModLoadingContext context) {
        ChunkEffectsState.register(context);

        var modBusGroup = context.getModBusGroup();
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(ChunkEffectsState::onConfigLoad);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(ChunkEffectsState::onConfigReload);

        // Keybinding registration (mod bus event)
        RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(event ->
                event.register(KeyBindings.OPEN_SCREEN));

        // Key input (game bus event)
        InputEvent.Key.BUS.addListener(event -> {
            if (KeyBindings.OPEN_SCREEN.consumeClick()
                    && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new ChunkEffectsScreen());
            }
        });

        // Player tick (game bus event)
        TickEvent.PlayerTickEvent.Post.BUS.addListener(this::onPlayerTick);
    }

    // ---------------------------------------------------------------
    // Server-side tick
    // ---------------------------------------------------------------
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        if (!ChunkEffectsState.isModEnabled) {
            TimedEffectManager mgr = timedManagers.get(player.getUUID());
            if (mgr != null) mgr.reset(player);
            return;
        }

        switch (ChunkEffectsState.currentMode) {
            case CHUNK_CHAOS -> tickChunkChaos(player);
            case TIMED_CHAOS -> tickTimedChaos(player);
        }
    }

    private void tickChunkChaos(Player player) {
        TimedEffectManager mgr = timedManagers.get(player.getUUID());
        if (mgr != null) {
            mgr.reset(player);
            timedManagers.remove(player.getUUID());
        }

        long chunkPos = getChunkKey(player);

        ChunkEffectData effectData = chunkEffects.computeIfAbsent(chunkPos, k -> {
            List<Holder<MobEffect>> allEffects = new ArrayList<>(
                    BuiltInRegistries.MOB_EFFECT.stream()
                            .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                            .toList()
            );
            Holder<MobEffect> randomEffect = allEffects.get(RANDOM.nextInt(allEffects.size()));
            int randomAmplifier = RANDOM.nextInt(MAX_AMPLIFIER + 1);
            return new ChunkEffectData(randomEffect, randomAmplifier);
        });

        player.addEffect(new MobEffectInstance(
                effectData.effect,
                40,
                effectData.amplifier,
                false,
                false,
                true
        ));
    }

    private void tickTimedChaos(Player player) {
        TimedEffectManager mgr = timedManagers.computeIfAbsent(
                player.getUUID(), id -> new TimedEffectManager());
        mgr.tick(player);
    }

    private long getChunkKey(Player player) {
        int chunkX = (int) player.getX() >> 4;
        int chunkZ = (int) player.getZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}