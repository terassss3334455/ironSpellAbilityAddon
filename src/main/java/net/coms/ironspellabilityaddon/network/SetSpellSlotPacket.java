package net.coms.ironspellabilityaddon.network;

import net.coms.ironspellabilityaddon.AddonRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSpellSlotPacket(int slot, String spellId, int level) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(slot);
        buffer.writeUtf(spellId == null ? "" : spellId, 256);
        buffer.writeInt(level);
    }
    public static SetSpellSlotPacket decode(FriendlyByteBuf buffer) {
        return new SetSpellSlotPacket(buffer.readInt(), buffer.readUtf(256), buffer.readInt());
    }
    public static void handle(SetSpellSlotPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && AddonRules.isValidSlotUpdate(packet.slot, packet.spellId, packet.level)) {
                AddonNetwork.updateSlot(player, packet.slot, packet.spellId, packet.level);
            }
        });
        context.setPacketHandled(true);
    }
}
