package net.coms.ironspellabilityaddon.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class CooldownSyncPacket {
    private final CompoundTag data;

    public CooldownSyncPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public void encode(FriendlyByteBuf buffer) { buffer.writeNbt(data); }
    public static CooldownSyncPacket decode(FriendlyByteBuf buffer) {
        return new CooldownSyncPacket(buffer.readNbt());
    }
    public static void handle(CooldownSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.coms.ironspellabilityaddon.client.ClientBridge.applyCooldowns(packet.data)));
        context.setPacketHandled(true);
    }
}
