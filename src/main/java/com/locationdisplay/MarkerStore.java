package com.locationdisplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class MarkerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<Marker> markers = new ArrayList<>();
    public static boolean visible = true;
    private static Path file;

    private MarkerStore() {}

    public static void load(MinecraftClient c) {
        String key = "unknown";
        try {
            if (c.getServer() != null) key = c.getServer().getSaveProperties().getLevelName();
            else if (c.getCurrentServerEntry() != null) key = c.getCurrentServerEntry().address;
        } catch (Exception e) {
            LocationDisplayClient.LOGGER.warn("Could not determine world/server name", e);
        }
        key = key == null ? "" : key.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (key.isBlank() || key.chars().allMatch(ch -> ch == '.')) key = "unknown";
        if (key.length() > 100) key = key.substring(0, 100);

        file = FabricLoader.getInstance().getConfigDir().resolve("locationdisplay").resolve(key + ".json");
        markers.clear();
        if (!Files.exists(file)) return;

        try {
            List<Marker> list = GSON.fromJson(Files.readString(file), new TypeToken<List<Marker>>() {}.getType());
            if (list != null) {
                for (Marker m : list) if (m != null && m.sanitize()) markers.add(m);
            }
        } catch (Exception e) {
            // Don't let the next save silently overwrite a file we couldn't read.
            LocationDisplayClient.LOGGER.error("Could not read markers from {}; keeping a backup", file, e);
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException io) {
                LocationDisplayClient.LOGGER.error("Could not back up corrupt marker file", io);
            }
            markers.clear();
        }
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            // write to a temp file first so a crash mid-write can't corrupt existing markers
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(markers));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LocationDisplayClient.LOGGER.error("Could not save markers", e);
        }
    }
}
