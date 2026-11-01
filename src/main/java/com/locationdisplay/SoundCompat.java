package com.locationdisplay;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.lang.reflect.Method;

/**
 * The static "play a UI sound" factory on PositionedSoundInstance is called `master` in older Yarn
 * and `ui` in 1.21.11, but it is the same method (intermediary method_4758) in every 1.21.x.
 * Look it up through the mapping resolver so one source file works everywhere.
 */
final class SoundCompat {
    private static Method factory;
    private static boolean resolved;

    private SoundCompat() {}

    static void playChime(MinecraftClient c) {
        if (!resolved) {
            resolved = true;
            try {
                String name = FabricLoader.getInstance().getMappingResolver().mapMethodName(
                        "intermediary", "net.minecraft.class_1109", "method_4758",
                        "(Lnet/minecraft/class_3414;F)Lnet/minecraft/class_1109;");
                factory = PositionedSoundInstance.class.getMethod(name, SoundEvent.class, float.class);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LocationDisplayClient.LOGGER.warn("UI sound factory not found; marker sound disabled", e);
            }
        }
        if (factory == null) return;
        try {
            c.getSoundManager().play((SoundInstance) factory.invoke(null, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // a missing sound effect must never break adding a marker
        }
    }
}
