package com.locationdisplay;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Minecraft 1.21 - 1.21.5: string keybind category, HudRenderCallback. */
final class Compat {
    private Compat() {}

    static KeyBinding registerKey(String translationKey, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey, InputUtil.Type.KEYSYM, keyCode, "key.category.locationdisplay.main"));
    }

    @SuppressWarnings("deprecation")
    static void registerHud(HudRenderer renderer) {
        HudRenderCallback.EVENT.register((g, tickCounter) -> renderer.render(g));
    }
}
