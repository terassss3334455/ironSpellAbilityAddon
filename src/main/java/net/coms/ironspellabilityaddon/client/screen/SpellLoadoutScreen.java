package net.coms.ironspellabilityaddon.client.screen;

import net.coms.ironspellabilityaddon.client.ClientState;
import net.coms.ironspellabilityaddon.client.ClientState.SlotView;
import net.coms.ironspellabilityaddon.network.AddonNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpellLoadoutScreen extends Screen {
    private int selectedSlot;
    private int page;

    public SpellLoadoutScreen() {
        super(Component.translatable("screen.ironspellabilityaddon.loadout"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(500, this.width - 16);
        int panelHeight = Math.min(286, this.height - 16);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int slotWidth = Math.min(190, (panelWidth - 30) / 2);
        int listLeft = left + slotWidth + 20;
        int listWidth = panelWidth - slotWidth - 30;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xE9101420);
        graphics.fill(left, top, left + panelWidth, top + 26, 0xFF27364C);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + 8, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.choose_slot"),
                left + 10, top + 36, 0xC8D5EA);

        for (int i = 0; i < 6; i++) {
            int rowY = top + 54 + i * 27;
            boolean selected = i == selectedSlot;
            graphics.fill(left + 8, rowY - 3, left + slotWidth, rowY + 20,
                    selected ? 0xFF405A7B : 0xFF202A39);
            SlotView slot = ClientState.slots().get(i);
            String label = slot.empty() ? Component.translatable("screen.ironspellabilityaddon.empty").getString()
                    : slot.title() + " (" + slot.level() + ")";
            graphics.drawString(font, Component.literal((i + 1) + ". " + trim(label, 22)),
                    left + 14, rowY + 3, selected ? 0xFFFFD66B : 0xFFFFFFFF);
            graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.clear"),
                    left + slotWidth - 50, rowY + 3, 0xFFEF9A9A);
        }

        graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.learned"),
                listLeft, top + 36, 0xFFC8D5EA);
        int rowsPerPage = 8;
        int start = page * rowsPerPage;
        int end = Math.min(ClientState.known().size(), start + rowsPerPage);
        if (ClientState.known().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.no_learned"),
                    listLeft, top + 62, 0xFFFFD66B);
        } else {
            for (int i = start; i < end; i++) {
                var spell = ClientState.known().get(i);
                int rowY = top + 54 + (i - start) * 25;
                graphics.fill(listLeft - 3, rowY - 3, listLeft + listWidth, rowY + 20,
                        mouseX >= listLeft - 3 && mouseX <= listLeft + listWidth
                                && mouseY >= rowY - 3 && mouseY <= rowY + 20
                                ? 0xFF405A7B : 0xFF202A39);
                graphics.drawString(font, Component.literal(trim(spell.title()
                                + "  Lv." + spell.level(), 28)),
                        listLeft + 4, rowY + 3, 0xFFFFFFFF);
            }
        }

        int pages = Math.max(1, (ClientState.known().size() + rowsPerPage - 1) / rowsPerPage);
        graphics.drawString(font, Component.literal((page + 1) + " / " + pages),
                listLeft + Math.max(0, (listWidth - 28) / 2), top + panelHeight - 30, 0xFFD8E2F0);
        graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.previous"),
                listLeft, top + panelHeight - 30, page > 0 ? 0xFFFFFFFF : 0xFF7B8391);
        graphics.drawString(font, Component.translatable("screen.ironspellabilityaddon.next"),
                left + panelWidth - 68, top + panelHeight - 30,
                page + 1 < pages ? 0xFFFFFFFF : 0xFF7B8391);
        graphics.drawCenteredString(font,
                Component.translatable("screen.ironspellabilityaddon.config_hint"),
                left + panelWidth / 2, top + panelHeight - 14, 0xFFB6C3D5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int panelWidth = Math.min(500, this.width - 16);
        int panelHeight = Math.min(286, this.height - 16);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int slotWidth = Math.min(190, (panelWidth - 30) / 2);
        int listLeft = left + slotWidth + 20;
        int listWidth = panelWidth - slotWidth - 30;

        for (int i = 0; i < 6; i++) {
            int rowY = top + 54 + i * 27;
            if (mouseY >= rowY - 3 && mouseY <= rowY + 20
                    && mouseX >= left + 8 && mouseX <= left + slotWidth) {
                if (mouseX >= left + slotWidth - 56) AddonNetwork.setSlot(i, "", 0);
                else selectedSlot = i;
                return true;
            }
        }

        int rowsPerPage = 8;
        int start = page * rowsPerPage;
        int end = Math.min(ClientState.known().size(), start + rowsPerPage);
        for (int i = start; i < end; i++) {
            int rowY = top + 54 + (i - start) * 25;
            if (mouseX >= listLeft - 3 && mouseX <= listLeft + listWidth
                    && mouseY >= rowY - 3 && mouseY <= rowY + 20) {
                var scroll = ClientState.known().get(i);
                AddonNetwork.setSlot(selectedSlot, scroll.id(), scroll.level());
                return true;
            }
        }

        if (mouseY >= top + panelHeight - 36 && mouseY <= top + panelHeight - 18) {
            if (mouseX < listLeft + listWidth / 2 && page > 0) page--;
            else if (mouseX >= listLeft + listWidth / 2
                    && (page + 1) * rowsPerPage < ClientState.known().size()) page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int pages = Math.max(1, (ClientState.known().size() + 7) / 8);
        page = Math.max(0, Math.min(pages - 1, page + (delta < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static String trim(String text, int limit) {
        if (text == null) return "";
        return text.length() <= limit ? text : text.substring(0, Math.max(0, limit - 1)) + "…";
    }
}
