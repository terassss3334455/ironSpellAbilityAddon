package net.coms.ironspellabilityaddon.client;

import net.coms.ironspellabilityaddon.IronSpellAbilityAddon;
import net.coms.ironspellabilityaddon.client.screen.SpellAbilityWheel;
import net.coms.ironspellabilityaddon.network.AddonNetwork;
import net.coms.ironspellabilityaddon.client.ClientState.SlotView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = IronSpellAbilityAddon.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean requestedInitialData;

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientState.tick();
        if (minecraft.player == null || minecraft.level == null) {
            requestedInitialData = false;
            return;
        }

        if (!requestedInitialData) {
            requestedInitialData = true;
            AddonNetwork.requestData(-1);
        }

        if (minecraft.screen == null) {
            if (ClientKeys.CONFIGURE.consumeClick()) AddonNetwork.requestData(0);
        } else {
            while (ClientKeys.CONFIGURE.consumeClick()) { }
        }
        while (ClientKeys.CAST.consumeClick()) { }
        if (SpellAbilityWheel.isActive()
                && (minecraft.player == null || minecraft.level == null || minecraft.screen != null)) {
            SpellAbilityWheel.closeWithoutCast();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!ClientKeys.CAST.matches(event.getKey(), event.getScanCode())) return;
        handleCastButtonAction(event.getAction());
    }

    @SubscribeEvent
    public static void onMouseButtonInput(InputEvent.MouseButton.Pre event) {
        if (!ClientKeys.CAST.matchesMouse(event.getButton())) return;
        handleCastButtonAction(event.getAction());
        // Side buttons are used as a hold-to-open modifier for the wheel, not as a
        // vanilla back/forward action while the player is in the world.
        if (event.getButton() >= 3 && Minecraft.getInstance().screen == null) event.setCanceled(true);
    }

    private static void handleCastButtonAction(int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (action == GLFW.GLFW_PRESS) {
            if (minecraft.screen != null) return;
            if (!ClientState.enabled()) {
                AddonNetwork.requestData(0);
                return;
            }
            SpellAbilityWheel.open();
        } else if (action == GLFW.GLFW_RELEASE && SpellAbilityWheel.isActive()) {
            int slot = SpellAbilityWheel.closeAndGetSelection(true);
            if (slot >= 0) AddonNetwork.castSlot(slot);
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SpellAbilityWheel.render(event.getGuiGraphics());

        List<SlotView> active = new ArrayList<>();
        for (SlotView slot : ClientState.slots()) {
            if (!slot.empty() && slot.remaining() > 0) active.add(slot);
        }
        if (active.isEmpty()) return;

        int right = minecraft.getWindow().getGuiScaledWidth() - 8;
        int y = 8;
        for (SlotView slot : active) {
            String label = slot.title() + "  "
                    + String.format(java.util.Locale.ROOT, "%.1f s", slot.remaining() / 20.0F);
            int width = minecraft.font.width(label);
            int x = right - width;
            event.getGuiGraphics().fill(x - 4, y - 2, right + 4, y + 10, 0x990B1018);
            event.getGuiGraphics().drawString(minecraft.font, Component.literal(label), x, y, 0xFFFFD66B);
            y += 14;
        }
    }
}
