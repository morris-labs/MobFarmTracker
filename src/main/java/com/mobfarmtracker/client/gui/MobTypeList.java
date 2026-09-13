package com.mobfarmtracker.client.gui;

import com.mobfarmtracker.client.ClientMobStats;
import com.mobfarmtracker.network.MobTypeStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/** Scrollable per-entity-type breakdown of current count and per-second spawn/kill/cramming rates. */
final class MobTypeList extends ObjectSelectionList<MobTypeList.Row> {
    private static final int NAME_X = 6;
    private static final int COUNT_X = 150;
    private static final int SPAWN_X = 200;
    private static final int KILL_X = 260;
    private static final int CRAMMING_X = 320;
    private static final int LIFESPAN_X = 380;

    MobTypeList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    void refresh() {
        List<MobTypeStats> stats = ClientMobStats.getPerType();
        Row selected = getSelected();
        replaceEntries(stats.stream().map(Row::new).toList());
        setSelected(selected);
    }

    final class Row extends ObjectSelectionList.Entry<Row> {
        private final MobTypeStats stats;
        private final Component name;

        Row(MobTypeStats stats) {
            this.stats = stats;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(stats.typeId());
            this.name = type.getDescription();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                            int mouseX, int mouseY, boolean hovering, float partialTick) {
            var font = Minecraft.getInstance().font;
            int textY = top + (height - font.lineHeight) / 2;
            guiGraphics.drawString(font, name, left + NAME_X, textY, 0xFFFFFF, false);
            guiGraphics.drawString(font, String.valueOf(stats.currentCount()), left + COUNT_X, textY, 0xFFFFFF, false);
            guiGraphics.drawString(font, "%.2f".formatted(ClientMobStats.perSecond(stats.spawnCount())), left + SPAWN_X, textY, 0x7FFF7F, false);
            guiGraphics.drawString(font, "%.2f".formatted(ClientMobStats.perSecond(stats.nonCrammingDeathCount())), left + KILL_X, textY, 0xFF7F7F, false);
            guiGraphics.drawString(font, "%.2f".formatted(ClientMobStats.perSecond(stats.crammingDeathCount())), left + CRAMMING_X, textY, 0xFFC080, false);
            String lifespan = stats.avgLifespanSeconds() > 0 ? "%.1fs".formatted(stats.avgLifespanSeconds()) : "-";
            guiGraphics.drawString(font, lifespan, left + LIFESPAN_X, textY, 0xA0C0FF, false);
        }

        @Override
        public Component getNarration() {
            return name;
        }
    }
}
