package com.locationdisplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MarkerScreen extends Screen {
    static final int[] COLORS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF, 0xFFFFFF};
    static final String[] ICONS = {"minecraft:red_bed", "minecraft:wheat", "minecraft:chest", "minecraft:diamond_pickaxe",
            "minecraft:spawner", "minecraft:nether_star", "minecraft:iron_ingot", "minecraft:oak_sapling",
            "minecraft:beacon", "minecraft:ender_pearl"};
    private static final int MAX_ROWS = 6;

    private String draftName = "";
    private int colorIdx = 0, iconIdx = 0;
    private TextFieldWidget nameField;

    public MarkerScreen() { super(Text.literal("Location Display")); }

    @Override
    protected void init() {
        int cx = width / 2;
        nameField = new TextFieldWidget(textRenderer, cx - 100, 30, 200, 20, Text.literal("Name"));
        nameField.setPlaceholder(Text.literal("Marker name"));
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

        int y = 115;
        for (int i = 0; i < Math.min(MAX_ROWS, MarkerStore.markers.size()); i++) {
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
            y += 24;
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
        c.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f));
        draftName = "";
        clearAndInit();
    }

    private static Text colorText(int rgb) {
        return Text.literal("Color ■").styled(s -> s.withColor(rgb));
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
        g.drawItem(new ItemStack(Registries.ITEM.get(Identifier.of(ICONS[iconIdx]))), cx + 84, 57);
        int y = 115;
        for (int i = 0; i < Math.min(MAX_ROWS, MarkerStore.markers.size()); i++) {
            Marker m = MarkerStore.markers.get(i);
            g.drawItem(new ItemStack(Registries.ITEM.get(Identifier.of(m.icon))), cx - 100, y + 2);
            g.drawTextWithShadow(textRenderer, Text.literal(m.name), cx - 80, y + 6, 0xFF000000 | m.color);
            y += 24;
        }
        if (MarkerStore.markers.size() > MAX_ROWS)
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("+" + (MarkerStore.markers.size() - MAX_ROWS) + " more"), cx, y + 2, 0xFFAAAAAA);
    }
}
