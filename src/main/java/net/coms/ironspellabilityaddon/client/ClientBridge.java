package net.coms.ironspellabilityaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.coms.ironspellabilityaddon.client.screen.SpellLoadoutScreen;

public final class ClientBridge {
    private ClientBridge() {}

    public static void applySpellData(CompoundTag data) {
        ClientState.apply(data);
        Minecraft minecraft = Minecraft.getInstance();
        String status = ClientState.status();
        if (!status.isEmpty() && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(status), true);
        }

        if (ClientState.mode() == 0 && !(minecraft.screen instanceof SpellLoadoutScreen)) {
            minecraft.setScreen(new SpellLoadoutScreen());
        }
    }

    public static void applyCooldowns(CompoundTag data) {
        ClientState.applyCooldowns(data);
    }
}
