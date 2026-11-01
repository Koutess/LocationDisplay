package com.locationdisplay;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Caches icon ItemStacks so we don't parse an Identifier and allocate a stack per marker per frame. */
final class Icons {
    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private Icons() {}

    static ItemStack get(String id) {
        return CACHE.computeIfAbsent(id == null ? "" : id, Icons::make);
    }

    private static ItemStack make(String id) {
        Identifier parsed = Identifier.tryParse(id);
        Item item = parsed == null ? Items.AIR : Registries.ITEM.get(parsed);
        if (item == Items.AIR) item = Items.BARRIER; // unknown / removed item: show something visible
        return new ItemStack(item);
    }
}
