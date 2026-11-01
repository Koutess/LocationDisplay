package com.locationdisplay;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/** Minecraft 1.21.9 - 1.21.11: KeyBinding.Category, HudElementRegistry. */
final class Compat {
    private Compat() {}

    static KeyBinding registerKey(String translationKey, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey, InputUtil.Type.KEYSYM, keyCode,
                KeyBinding.Category.create(Identifier.of("locationdisplay", "main"))));
    }

    static void registerHud(HudRenderer renderer) {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("locationdisplay", "markers"), (g, tickCounter) -> renderer.render(g));
    }
}
