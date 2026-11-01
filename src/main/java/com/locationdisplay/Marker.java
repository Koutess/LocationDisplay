package com.locationdisplay;

import net.minecraft.util.Identifier;

public class Marker {
    public static final String DEFAULT_ICON = "minecraft:red_bed";

    public String name;
    public int color;      // RGB
    public String icon;    // item id, e.g. minecraft:red_bed
    public double x, y, z;
    public String dimension;

    /** Repairs hand-edited / old data. Returns false if the marker is unusable and should be dropped. */
    boolean sanitize() {
        if (dimension == null || Identifier.tryParse(dimension) == null) return false;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        if (name == null || name.isBlank()) name = "Marker";
        if (icon == null || Identifier.tryParse(icon) == null) icon = DEFAULT_ICON;
        color &= 0xFFFFFF;
        return true;
    }
}
