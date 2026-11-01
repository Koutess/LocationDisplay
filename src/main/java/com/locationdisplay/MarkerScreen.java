package com.locationdisplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class MarkerScreen extends Screen {
    static final int[] COLORS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF, 0xFFFFFF};
    static final String[] ICONS = {"minecraft:red_bed", "minecraft:wheat", "minecraft:chest", "minecraft:diamond_pickaxe",
            "minecraft:spawner", "minecraft:nether_star", "minecraft:iron_ingot", "minecraft:oak_sapling",
            "minecraft:beacon", "minecraft:ender_pearl"};

    private static final int LIST_Y = 134, ROW_H = 24, FOOTER_H = 30, NAME_WIDTH = 88;

    private String draftName = "";
    private int colorIdx = 0, iconIdx = 0, page = 0;
    private TextFieldWidget nameField;

    public MarkerScreen() { super(Text.literal("Location Display")); }

    private int rowsPerPage() {
        return Math.max(1, Math.min(8, (height - LIST_Y - FOOTER_H) / ROW_H));
    }

    private int pageCount() {
        return Math.max(1, (MarkerStore.markers.size() + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        page = Math.max(0, Math.min(page, pageCount() - 1));

        nameField = new TextFieldWidget(textRenderer, cx - 100, 30, 200, 20, Text.literal("Name"));
        nameField.setPlaceholder(Text.literal("Marker name"));
        nameField.setMaxLength(32);
        nameField.setText(draftName);
        nameField.setChangedListener(s -> draftName = s);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(colorText(COLORS[colorIdx]), b -> {
            colorIdx = (colorIdx + 1) % COLORS.length;
            b.setMessage(colorText(COLORS[colorIdx]));
        }).dimensions(cx - 100, 55, 98, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Icon"), b -> iconIdx = (iconIdx + 1) % ICONS.length)
                .dimensions(cx + 2, 55, 78, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add marker here"), b -> addMarker())
                .dimensions(cx - 100, 80, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(visibilityText(), b -> {
            MarkerStore.visible = !MarkerStore.visible;
            b.setMessage(visibilityText());
        }).dimensions(cx - 100, 104, 200, 20).build());

        int perPage = rowsPerPage();
        int first = page * perPage;
        int y = LIST_Y;
        for (int i = first; i < Math.min(first + perPage, MarkerStore.markers.size()); i++) {
            final Marker m = MarkerStore.markers.get(i);
            addDrawableChild(ButtonWidget.builder(colorText(m.color), b -> {
                m.color = COLORS[(indexOfColor(m.color) + 1) % COLORS.length];
                MarkerStore.save();
                clearAndInit();
            }).dimensions(cx + 10, y, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Icon"), b -> {
                m.icon = ICONS[(indexOfIcon(m.icon) + 1) % ICONS.length];
                MarkerStore.save();
            }).dimensions(cx + 36, y, 36, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                MarkerStore.markers.remove(m);
                MarkerStore.save();
                clearAndInit();
            }).dimensions(cx + 74, y, 26, 20).build());
            y += ROW_H;
        }

        if (pageCount() > 1) {
            int py = height - 26;
            ButtonWidget prev = ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); })
                    .dimensions(cx - 100, py, 24, 20).build();
            ButtonWidget next = ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); })
                    .dimensions(cx + 76, py, 24, 20).build();
            prev.active = page > 0;
            next.active = page < pageCount() - 1;
            addDrawableChild(prev);
            addDrawableChild(next);
        }
    }

    private void addMarker() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.world == null) return;
        Marker m = new Marker();
        m.name = draftName.isBlank() ? "Marker " + (MarkerStore.markers.size() + 1) : draftName.trim();
        m.color = COLORS[colorIdx];
        m.icon = ICONS[iconIdx];
        m.x = Math.floor(c.player.getX()) + 0.5;
        m.y = Math.floor(c.player.getY());
        m.z = Math.floor(c.player.getZ()) + 0.5;
        m.dimension = c.world.getRegistryKey().getValue().toString();
        MarkerStore.markers.add(m);
        MarkerStore.save();
        SoundCompat.playChime(c);
        draftName = "";
        page = Integer.MAX_VALUE; // jump to the page with the new marker (clamped in init)
        clearAndInit();
    }

    private static Text colorText(int rgb) {
        return Text.literal("Color ■").styled(s -> s.withColor(rgb));
    }

    private static Text visibilityText() {
        return Text.literal("Markers: " + (MarkerStore.visible ? "shown" : "hidden"));
    }

    private static int indexOfColor(int c) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == c) return i;
        return 0;
    }

    private static int indexOfIcon(String id) {
        for (int i = 0; i < ICONS.length; i++) if (ICONS[i].equals(id)) return i;
        return 0;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = width / 2;
        g.drawCenteredTextWithShadow(textRenderer, title, cx, 12, 0xFFFFFFFF);
        g.drawItem(Icons.get(ICONS[iconIdx]), cx + 84, 57);

        String currentDim = client != null && client.world != null
                ? client.world.getRegistryKey().getValue().toString() : null;

        int perPage = rowsPerPage();
        int first = page * perPage;
        int y = LIST_Y;
        for (int i = first; i < Math.min(first + perPage, MarkerStore.markers.size()); i++) {
            Marker m = MarkerStore.markers.get(i);
            g.drawItem(Icons.get(m.icon), cx - 100, y + 2);
            // markers from other dimensions are greyed out (they aren't shown in the HUD here)
            int nameColor = m.dimension != null && m.dimension.equals(currentDim) ? 0xFF000000 | m.color : 0xFF888888;
            g.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(m.name, NAME_WIDTH)), cx - 80, y + 6, nameColor);
            y += ROW_H;
        }

        if (MarkerStore.markers.isEmpty()) {
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("No markers yet"), cx, LIST_Y + 6, 0xFFAAAAAA);
        } else if (pageCount() > 1) {
            g.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Page " + (page + 1) + "/" + pageCount() + "  (" + MarkerStore.markers.size() + " markers)"),
                    cx, height - 20, 0xFFAAAAAA);
        }
    }
}
