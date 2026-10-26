package net.coms.ironspellabilityaddon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestSpellDataPacket(int mode) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeInt(mode); }
    public static RequestSpellDataPacket decode(FriendlyByteBuf buffer) {
        return new RequestSpellDataPacket(buffer.readInt());
    }
    public static void handle(RequestSpellDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && (packet.mode == -1 || packet.mode == 0 || packet.mode == 1)) {
                AddonNetwork.openData(player, packet.mode);
            }
        });
        context.setPacketHandled(true);
    }
}
