package net.coms.ironspellabilityaddon.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.coms.ironspellabilityaddon.client.ClientState;
import net.coms.ironspellabilityaddon.client.ClientState.SlotView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A small, translucent hold-to-open wheel inspired by Iron's spell wheel. */
public final class SpellAbilityWheel {
    private static final int SLOT_COUNT = 6;
    private static final double INNER_RADIUS = 34;
    private static final double OUTER_RADIUS = 101;
    private static boolean active;
    private static int selected;

    private SpellAbilityWheel() {}

    public static boolean isActive() { return active; }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active || minecraft.player == null || minecraft.screen != null || !ClientState.enabled()) return;
        active = true;
        selected = firstConfiguredSlot();
        minecraft.mouseHandler.releaseMouse();
        updateSelection();
    }

    /** Closes the wheel and returns the selected slot, or -1 when no spell is selected. */
    public static int closeAndGetSelection(boolean confirm) {
        if (!active) return -1;
        updateSelection();
        int result = confirm && !ClientState.slots().get(selected).empty() ? selected : -1;
        active = false;
        Minecraft.getInstance().mouseHandler.grabMouse();
        return result;
    }

    public static void closeWithoutCast() {
        closeAndGetSelection(false);
    }

    public static void render(GuiGraphics graphics) {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !ClientState.enabled()) {
            closeWithoutCast();
            return;
        }
        updateSelection();

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        drawWheel(centerX, centerY);

        for (int i = 0; i < SLOT_COUNT; i++) {
            SlotView slot = ClientState.slots().get(i);
            double angle = -Math.PI / 2 + i * Math.PI / 3;
            int x = centerX + (int) Math.round(Math.cos(angle) * 68);
            int y = centerY + (int) Math.round(Math.sin(angle) * 68);
            graphics.drawCenteredString(minecraft.font, Integer.toString(i + 1), x, y - 21, 0xFFE9E1CE);
            if (slot.empty()) continue;

            ResourceLocation spellId = ResourceLocation.tryParse(slot.id());
            var spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null && spell.getSpellIconResource() != null) {
                graphics.blit(spell.getSpellIconResource(), x - 8, y - 8,
                        0, 0, 16, 16, 16, 16);
            } else {
                graphics.drawCenteredString(minecraft.font, "?", x, y - 4, 0xFFFFFFFF);
            }
        }

        SlotView chosen = ClientState.slots().get(selected);
        String title = chosen.empty() ? Component.translatable("screen.ironspellabilityaddon.empty").getString()
                : chosen.title() + "  Lv." + chosen.level();
        graphics.fill(centerX - 94, centerY + 108, centerX + 94, centerY + 146, 0x990A0D12);
        graphics.drawCenteredString(minecraft.font, title, centerX, centerY + 114, 0xFFFFFFFF);
        String cooldown = chosen.empty() ? "" : chosen.remaining() > 0
                ? String.format(java.util.Locale.ROOT, "%.1f s", chosen.remaining() / 20.0F)
                : Component.translatable("screen.ironspellabilityaddon.ready").getString();
        graphics.drawCenteredString(minecraft.font, cooldown, centerX, centerY + 129,
                chosen.remaining() > 0 ? 0xFFFFC66D : 0xFF9DE3A8);
    }

    private static void updateSelection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        double mouseX = minecraft.mouseHandler.xpos() * guiWidth / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * guiHeight / minecraft.getWindow().getScreenHeight();
        double dx = mouseX - guiWidth / 2.0;
        double dy = mouseY - guiHeight / 2.0;
        if (dx * dx + dy * dy < INNER_RADIUS * INNER_RADIUS) return;

        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += Math.PI * 2;
        selected = Mth.floor((angle + Math.PI / 6) / (Math.PI / 3)) % SLOT_COUNT;
    }

    private static int firstConfiguredSlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!ClientState.slots().get(i).empty()) return i;
        }
        return 0;
    }

    private static void drawWheel(int centerX, int centerY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            double center = -Math.PI / 2 + slot * Math.PI / 3;
            double start = center - Math.PI / 6 + 0.025;
            double end = center + Math.PI / 6 - 0.025;
            boolean highlighted = slot == selected;
            float r = highlighted ? 0.62f : 0.20f;
            float g = highlighted ? 0.50f : 0.25f;
            float b = highlighted ? 0.30f : 0.32f;
            float alpha = highlighted ? 0.58f : 0.30f;

            int segments = 12;
            for (int part = 0; part < segments; part++) {
                double a1 = start + (end - start) * part / segments;
                double a2 = start + (end - start) * (part + 1) / segments;
                buffer.vertex(centerX + Math.cos(a1) * INNER_RADIUS,
                        centerY + Math.sin(a1) * INNER_RADIUS, 0).color(r, g, b, alpha).endVertex();
                buffer.vertex(centerX + Math.cos(a2) * INNER_RADIUS,
                        centerY + Math.sin(a2) * INNER_RADIUS, 0).color(r, g, b, alpha).endVertex();
                buffer.vertex(centerX + Math.cos(a2) * OUTER_RADIUS,
                        centerY + Math.sin(a2) * OUTER_RADIUS, 0).color(r, g, b, 0.02f).endVertex();
                buffer.vertex(centerX + Math.cos(a1) * OUTER_RADIUS,
                        centerY + Math.sin(a1) * OUTER_RADIUS, 0).color(r, g, b, 0.02f).endVertex();
            }
        }
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}
