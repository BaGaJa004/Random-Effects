package net.bagaja.chunkeffects;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    // Category name shown in Options → Controls
    public static final String CATEGORY = "key.category.chunkeffects";

    // The keybinding itself — default key is K
    public static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.chunkeffects.open_screen",          // translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,                         // default: K
            CATEGORY
    );
}