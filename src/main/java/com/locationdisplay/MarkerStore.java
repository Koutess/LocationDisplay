package com.locationdisplay;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.*;
import java.util.*;

public final class MarkerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<Marker> markers = new ArrayList<>();
    public static boolean visible = true;
    private static Path file;

    public static void load(MinecraftClient c) {
        String key = "unknown";
        if (c.getServer() != null) key = c.getServer().getSaveProperties().getLevelName();
        else if (c.getCurrentServerEntry() != null) key = c.getCurrentServerEntry().address;
        key = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        file = FabricLoader.getInstance().getConfigDir().resolve("locationdisplay").resolve(key + ".json");
        markers.clear();
        try {
            if (Files.exists(file)) {
                List<Marker> list = GSON.fromJson(Files.readString(file), new TypeToken<List<Marker>>() {}.getType());
                if (list != null) markers.addAll(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(markers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
