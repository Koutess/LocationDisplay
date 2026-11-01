package com.locationdisplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

final class MarkerRenderer {
    private static final double NEAR = 0.05;
    private static final double LINE_HEIGHT = 4.0; // blocks

    private MarkerRenderer() {}

    /** Camera basis + projection parameters for one frame. */
    private static final class View {
        double ex, ey, ez;                 // camera position
        double fx, fy, fz;                 // forward
        double rx, rz;                     // right (horizontal)
        double ux, uy, uz;                 // up
        double cx, cy, focal;              // screen centre + focal length (scaled GUI pixels)

        /** World point -> camera space {right, up, forward}. */
        double[] toCamera(double x, double y, double z) {
            double dx = x - ex, dy = y - ey, dz = z - ez;
            return new double[]{dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
        }

        double sx(double[] p) { return cx + p[0] / p[2] * focal; }
        double sy(double[] p) { return cy - p[1] / p[2] * focal; }
    }

    static void render(DrawContext g) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (!MarkerStore.visible || MarkerStore.markers.isEmpty()) return;
        if (c.player == null || c.world == null || c.options.hudHidden) return;

        // Use the real render camera so third-person, sneaking, swimming etc. line up correctly.
        Camera cam = c.gameRenderer.getCamera();
        if (!cam.isReady()) return;
        Vec3d eye = CameraCompat.position(cam);
        if (eye == null) eye = c.player.getEyePos();

        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());

        int W = g.getScaledWindowWidth(), H = g.getScaledWindowHeight();
        View v = new View();
        v.ex = eye.x; v.ey = eye.y; v.ez = eye.z;
        // Minecraft: yaw 0 looks toward +Z
        v.fx = -Math.sin(yaw) * Math.cos(pitch);
        v.fy = -Math.sin(pitch);
        v.fz = Math.cos(yaw) * Math.cos(pitch);
        v.rx = -Math.cos(yaw);
        v.rz = -Math.sin(yaw);
        v.ux = -v.rz * v.fy;
        v.uy = v.rz * v.fx - v.rx * v.fz;
        v.uz = v.rx * v.fy;
        v.cx = W / 2.0;
        v.cy = H / 2.0;
        v.focal = (H / 2.0) / Math.tan(Math.toRadians(c.options.getFov().getValue()) / 2.0);

        String dim = c.world.getRegistryKey().getValue().toString();

        for (Marker m : MarkerStore.markers) {
            if (!dim.equals(m.dimension)) continue;

            double[] a = v.toCamera(m.x, m.y, m.z);
            double[] b = v.toCamera(m.x, m.y + LINE_HEIGHT, m.z);
            if (!clipToNearPlane(a, b)) continue; // fully behind the camera

            double ax = v.sx(a), ay = v.sy(a), bx = v.sx(b), by = v.sy(b);
            if (Math.max(ax, bx) < -50 || Math.min(ax, bx) > W + 50) continue;

            int color = 0xFF000000 | m.color;
            drawLine(g, ax, ay, bx, by, color, W, H);

            // label sits above the higher end of the line
            double lx = ay < by ? ax : bx;
            double ly = Math.min(ay, by);
            if (lx < -50 || lx > W + 50) continue;

            double dist = Math.sqrt(sq(m.x - c.player.getX()) + sq(m.y - c.player.getY()) + sq(m.z - c.player.getZ()));
            String label = m.name + " - " + formatDistance(dist);
            int half = c.textRenderer.getWidth(label) / 2 + 2;
            int tx = clamp((int) Math.round(lx), half, W - half);
            int ty = clamp((int) Math.round(ly) - 12, 24, H - 12);
            g.drawCenteredTextWithShadow(c.textRenderer, Text.literal(label), tx, ty, color);
            g.drawItem(Icons.get(m.icon), tx - 8, ty - 18);
        }
    }

    /** Clips the segment a-b (camera space, modified in place) to the near plane. */
    private static boolean clipToNearPlane(double[] a, double[] b) {
        boolean aIn = a[2] >= NEAR, bIn = b[2] >= NEAR;
        if (!aIn && !bIn) return false;
        if (aIn && bIn) return true;
        double[] in = aIn ? a : b, out = aIn ? b : a;
        double t = (NEAR - out[2]) / (in[2] - out[2]);
        for (int i = 0; i < 3; i++) out[i] += t * (in[i] - out[i]);
        out[2] = NEAR;
        return true;
    }

    /** 1px line from (ax,ay) to (bx,by), clamped to the screen. Handles slanted lines (looking up/down). */
    private static void drawLine(DrawContext g, double ax, double ay, double bx, double by, int color, int W, int H) {
        double dx = bx - ax, dy = by - ay;
        if (Math.abs(dx) > Math.abs(dy)) {
            int x0 = (int) Math.max(0, Math.floor(Math.min(ax, bx)));
            int x1 = (int) Math.min(W, Math.ceil(Math.max(ax, bx)));
            for (int x = x0; x < x1; x++) {
                int y = (int) Math.round(ay + (x + 0.5 - ax) / dx * dy);
                if (y >= 0 && y < H) g.fill(x, y, x + 1, y + 1, color);
            }
        } else {
            int y0 = (int) Math.max(0, Math.floor(Math.min(ay, by)));
            int y1 = (int) Math.min(H, Math.ceil(Math.max(ay, by)));
            if (dy == 0) return;
            for (int y = y0; y < y1; y++) {
                int x = (int) Math.round(ax + (y + 0.5 - ay) / dy * dx);
                if (x >= 0 && x < W) g.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static String formatDistance(double d) {
        return d >= 1000 ? String.format(Locale.ROOT, "%.1fkm", d / 1000.0) : (int) d + "m";
    }

    private static double sq(double d) { return d * d; }
    private static int clamp(int v, int lo, int hi) { return hi < lo ? lo : Math.max(lo, Math.min(hi, v)); }
}
