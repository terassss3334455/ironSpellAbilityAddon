package net.coms.ironspellabilityaddon.network;

import net.coms.ironspellabilityaddon.AddonRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastSpellPacket(int slot) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeInt(slot); }
    public static CastSpellPacket decode(FriendlyByteBuf buffer) {
        return new CastSpellPacket(buffer.readInt());
    }
    public static void handle(CastSpellPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && AddonRules.isValidSlot(packet.slot)) {
                AddonNetwork.cast(player, packet.slot);
            }
        });
        context.setPacketHandled(true);
    }
}
