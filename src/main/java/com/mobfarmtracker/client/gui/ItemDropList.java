package com.mobfarmtracker.client.gui;

import com.mobfarmtracker.client.ClientMobStats;
import com.mobfarmtracker.network.ItemDropStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;


/** Scrollable per-item-type breakdown of drop rates. */
final class ItemDropList extends ObjectSelectionList<ItemDropList.Row> {
    private static final int NAME_X = 6;
    private static final int RATE_X = 250;

    ItemDropList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    void refresh() {
        replaceEntries(ClientMobStats.getPerItem().stream().map(Row::new).toList());
    }

    final class Row extends ObjectSelectionList.Entry<Row> {
        private final ItemDropStats stats;
        private final Component name;

        Row(ItemDropStats stats) {
            this.stats = stats;
            Item item = BuiltInRegistries.ITEM.get(stats.itemId());
            this.name = item != null ? item.getDescription() : Component.literal(stats.itemId().toString());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                            int mouseX, int mouseY, boolean hovering, float partialTick) {
            var font = Minecraft.getInstance().font;
            int textY = top + (height - font.lineHeight) / 2;
            guiGraphics.drawString(font, name, left + NAME_X, textY, 0xFFFFFF, false);
            String rate = "%.2f/s".formatted(ClientMobStats.perSecond(stats.dropCount()));
            guiGraphics.drawString(font, rate, left + RATE_X, textY, 0xFFD700, false);
        }

        @Override
        public Component getNarration() {
            return name;
        }
    }
}
