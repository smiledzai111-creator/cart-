package com.cartpvp.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CartPvpClient implements ClientModInitializer {
    public static KeyBinding macroKey;

    @Override
    public void onInitializeClient() {
        macroKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cartpvp.macro", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.cartpvp"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (macroKey.wasPressed()) CartMacro.start(client);
            CartMacro.tick(client);
        });
    }
}
