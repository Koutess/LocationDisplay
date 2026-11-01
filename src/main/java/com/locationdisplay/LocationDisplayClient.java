package com.locationdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationDisplayClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("locationdisplay");
    private static KeyBinding key;

    @Override
    public void onInitializeClient() {
        key = Compat.registerKey("key.locationdisplay.main", GLFW.GLFW_KEY_BACKSLASH);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MarkerStore.load(client));
        ClientTickEvents.END_CLIENT_TICK.register(LocationDisplayClient::tick);
        Compat.registerHud(MarkerRenderer::render);
    }

    private static void tick(MinecraftClient c) {
        while (key.wasPressed()) {
            if (isShiftDown(c)) {
                MarkerStore.visible = !MarkerStore.visible;
                if (c.player != null) {
                    c.player.sendMessage(Text.literal("Location markers " + (MarkerStore.visible ? "shown" : "hidden")), true);
                }
            } else if (c.currentScreen == null && c.player != null) {
                c.setScreen(new MarkerScreen());
            }
        }
    }

    private static boolean isShiftDown(MinecraftClient c) {
        long w = c.getWindow().getHandle();
        return GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
