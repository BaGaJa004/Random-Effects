package net.bagaja.chunkeffects;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ChunkEffectsState {

    public static boolean isModEnabled = true;
    public static ChunkEffectsMode currentMode = ChunkEffectsMode.CHUNK_CHAOS;
    public static int cycleTicks = 6000;

    public static int timedEffectTicksRemaining = 0;
    public static int timedRestTicksRemaining = 0;

    // ---------------------------------------------------------------
    // Config persistence
    // ---------------------------------------------------------------
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue CFG_ENABLED = BUILDER
            .comment("Whether the mod is enabled")
            .define("enabled", true);

    private static final ForgeConfigSpec.EnumValue<ChunkEffectsMode> CFG_MODE = BUILDER
            .comment("The active mode: CHUNK_CHAOS or TIMED_CHAOS")
            .defineEnum("mode", ChunkEffectsMode.CHUNK_CHAOS);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    /** Call once in your mod constructor to register the config. */
    public static void register(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "chunkeffects.toml");
    }

    // No @SubscribeEvent needed — registered via addListener in the mod constructor
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(ChunkEffectsMod.MODID)) {
            load();
        }
    }

    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(ChunkEffectsMod.MODID)) {
            load();
        }
    }

    public static void load() {
        isModEnabled = CFG_ENABLED.get();
        currentMode  = CFG_MODE.get();
    }

    /** Persist the current static field values back to disk. */
    public static void save() {
        CFG_ENABLED.set(isModEnabled);
        CFG_MODE.set(currentMode);
        SPEC.save();
    }

    private ChunkEffectsState() {}
}