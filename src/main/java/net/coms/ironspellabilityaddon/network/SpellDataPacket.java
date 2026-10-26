package net.coms.ironspellabilityaddon.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SpellDataPacket {
    private final CompoundTag data;

    public SpellDataPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public void encode(FriendlyByteBuf buffer) { buffer.writeNbt(data); }
    public static SpellDataPacket decode(FriendlyByteBuf buffer) {
        return new SpellDataPacket(buffer.readNbt());
    }
    public static void handle(SpellDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.coms.ironspellabilityaddon.client.ClientBridge.applySpellData(packet.data)));
        context.setPacketHandled(true);
    }
}
