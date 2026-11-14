package com.mobfarmtracker.client.gui;

import com.mobfarmtracker.client.ClientMobStats;
import com.mobfarmtracker.network.MobTypeStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;

/** Scrollable per-entity-type breakdown of XP drop rates within the current sample window. */
final class MobXpList extends ObjectSelectionList<MobXpList.Row> {
    private static final int NAME_X = 6;
    private static final int RATE_X = 250;

    MobXpList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    void refresh() {
        replaceEntries(
                ClientMobStats.getPerType().stream()
                        .filter(s -> s.xpDropped() > 0)
                        .sorted(Comparator.comparingInt(MobTypeStats::xpDropped).reversed())
                        .map(Row::new)
                        .toList());
    }

    final class Row extends ObjectSelectionList.Entry<Row> {
        private final MobTypeStats stats;
        private final Component name;

        Row(MobTypeStats stats) {
            this.stats = stats;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(stats.typeId());
            this.name = type != null ? type.getDescription() : Component.literal(stats.typeId().toString());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                            int mouseX, int mouseY, boolean hovering, float partialTick) {
            var font = Minecraft.getInstance().font;
            int textY = top + (height - font.lineHeight) / 2;
            guiGraphics.drawString(font, name, left + NAME_X, textY, 0xFFFFFF, false);
            String rate = "%.2f/s".formatted(ClientMobStats.getXpRatePerSecond(stats.xpDropped()));
            guiGraphics.drawString(font, rate, left + RATE_X, textY, 0x80FF80, false);
        }

        @Override
        public Component getNarration() {
            return name;
        }
    }
}
