package com.locationdisplay;

import net.minecraft.client.gui.DrawContext;

@FunctionalInterface
interface HudRenderer {
    void render(DrawContext g);
}
