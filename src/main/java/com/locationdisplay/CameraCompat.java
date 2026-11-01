package com.locationdisplay;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Camera#getPos was renamed to getCameraPos (an interface method with a different intermediary name)
 * in newer 1.21.x versions. Rather than guess which release switched, look the method up through
 * Fabric's mapping resolver using both intermediary names and use whichever exists.
 */
final class CameraCompat {
    private static final String DESC = "()Lnet/minecraft/class_243;"; // ()Vec3d
    private static MethodHandle handle;
    private static boolean resolved;

    private CameraCompat() {}

    /** Camera position, or null if it can't be determined on this version. */
    static Vec3d position(Camera camera) {
        if (!resolved) {
            resolved = true;
            handle = resolve();
            if (handle == null) LocationDisplayClient.LOGGER.warn("Could not find Camera position accessor; falling back to player eye position");
        }
        if (handle == null) return null;
        try {
            return (Vec3d) handle.invoke(camera);
        } catch (Throwable t) {
            return null;
        }
    }

    private static MethodHandle resolve() {
        MappingResolver r = FabricLoader.getInstance().getMappingResolver();
        String[][] candidates = {
                {"net.minecraft.class_4184", "method_19326"},               // Camera#getPos
                {"net.minecraft.class_11200$class_11297", "method_71156"}   // YawProvider#getCameraPos
        };
        for (String[] c : candidates) {
            try {
                String name = r.mapMethodName("intermediary", c[0], c[1], DESC);
                Method m = Camera.class.getMethod(name);
                return MethodHandles.publicLookup().unreflect(m);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try next candidate
            }
        }
        return null;
    }
}
