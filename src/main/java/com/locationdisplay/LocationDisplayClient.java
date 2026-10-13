package com.locationdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class LocationDisplayClient implements ClientModInitializer {
    private static KeyBinding key;

    @Override
    public void onInitializeClient() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.locationdisplay.main", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH,
                KeyBinding.Category.create(Identifier.of("locationdisplay", "main"))));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MarkerStore.load(client));

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (key.wasPressed()) {
                long w = c.getWindow().getHandle();
                boolean shift = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (shift) MarkerStore.visible = !MarkerStore.visible;
                else if (c.currentScreen == null && c.player != null) c.setScreen(new MarkerScreen());
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("locationdisplay", "markers"), LocationDisplayClient::render);
    }

    private static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (!MarkerStore.visible || c.player == null || c.world == null || c.options.hudHidden) return;

        float t = tc.getTickProgress(false);
        Vec3d eye = c.player.getLerpedPos(t).add(0, c.player.getStandingEyeHeight(), 0);
        double yaw = Math.toRadians(c.player.getYaw(t));
        double pitch = Math.toRadians(c.player.getPitch(t));

        // camera basis (Minecraft: yaw 0 looks toward +Z)
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;

        int W = g.getScaledWindowWidth(), H = g.getScaledWindowHeight();
        double focal = (H / 2.0) / Math.tan(Math.toRadians(c.options.getFov().getValue()) / 2.0);
        String dim = c.world.getRegistryKey().getValue().toString();

        for (Marker m : MarkerStore.markers) {
            if (!dim.equals(m.dimension)) continue;
            double[] base = project(m.x - eye.x, m.y - eye.y, m.z - eye.z, rx, rz, ux, uy, uz, fx, fy, fz, W, H, focal);
            double[] top = project(m.x - eye.x, m.y + 4 - eye.y, m.z - eye.z, rx, rz, ux, uy, uz, fx, fy, fz, W, H, focal);
            if (base == null || top == null) continue;

            int color = 0xFF000000 | m.color;
            int x = (int) base[0];
            if (x < -50 || x > W + 50) continue;
            int y1 = (int) Math.max(0, Math.min(H, top[1]));
            int y2 = (int) Math.max(0, Math.min(H, base[1]));
            g.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2), color); // 1px thin line

            double dist = Math.sqrt(Math.pow(m.x - eye.x, 2) + Math.pow(m.y - eye.y, 2) + Math.pow(m.z - eye.z, 2));
            int ty = Math.max(24, y1 - 12);
            g.drawCenteredTextWithShadow(c.textRenderer, Text.literal(m.name + " - " + (int) dist + "m"), x, ty, color);
            g.drawItem(new ItemStack(Registries.ITEM.get(Identifier.of(m.icon))), x - 8, ty - 18);
        }
    }

    private static double[] project(double dx, double dy, double dz, double rx, double rz,
                                    double ux, double uy, double uz, double fx, double fy, double fz,
                                    int W, int H, double focal) {
        double cz = dx * fx + dy * fy + dz * fz;
        if (cz < 0.05) return null; // behind camera
        double cx = dx * rx + dz * rz;
        double cy = dx * ux + dy * uy + dz * uz;
        return new double[]{W / 2.0 + cx / cz * focal, H / 2.0 - cy / cz * focal};
    }
}
