package com.anthonytamayo.event;

import com.anthonytamayo.BlockSortClient;
import com.anthonytamayo.gui.ColorScreen;
import com.anthonytamayo.gui.ColorGui;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_blocksort = "key.category.blocksort.blocksort";
    public static final String KEY_COLOUR_INV_OPEN = "key.blocksort.color_inv_open";
    public static KeyBinding colorInvKey;

    public static void registerKeyInputs(BlockSortClient blocksortClient) {
        if (blocksortClient == null) {
            throw new IllegalArgumentException("BlockSortClient cannot be null");
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null && colorInvKey != null && colorInvKey.wasPressed()) {
                client.setScreen(client.currentScreen == null ? new ColorScreen(new ColorGui(client, blocksortClient)) : null);
            }
        });

    }

    public static void register(BlockSortClient blocksortClient) {
        if (blocksortClient == null) {
            throw new IllegalArgumentException("BlockSortClient cannot be null");
        }
        colorInvKey = KeyBindingHelper.registerKeyBinding
                (new KeyBinding(KEY_COLOUR_INV_OPEN, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY_blocksort));
        registerKeyInputs(blocksortClient);
    }
}
