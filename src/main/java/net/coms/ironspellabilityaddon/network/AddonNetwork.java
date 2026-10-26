package net.coms.ironspellabilityaddon.network;

import net.coms.ironspellabilityaddon.IronSpellAbilityAddon;
import net.coms.ironspellabilityaddon.SpellAbilityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class AddonNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(IronSpellAbilityAddon.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    private AddonNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(nextId++, RequestSpellDataPacket.class,
                RequestSpellDataPacket::encode, RequestSpellDataPacket::decode,
                RequestSpellDataPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, SetSpellSlotPacket.class,
                SetSpellSlotPacket::encode, SetSpellSlotPacket::decode,
                SetSpellSlotPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, CastSpellPacket.class,
                CastSpellPacket::encode, CastSpellPacket::decode,
                CastSpellPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, SpellDataPacket.class,
                SpellDataPacket::encode, SpellDataPacket::decode,
                SpellDataPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, CooldownSyncPacket.class,
                CooldownSyncPacket::encode, CooldownSyncPacket::decode,
                CooldownSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void requestData(int mode) {
        CHANNEL.sendToServer(new RequestSpellDataPacket(mode));
    }

    public static void setSlot(int slot, String id, int level) {
        CHANNEL.sendToServer(new SetSpellSlotPacket(slot, id, level));
    }

    public static void castSlot(int slot) {
        CHANNEL.sendToServer(new CastSpellPacket(slot));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendCooldowns(ServerPlayer player) {
        sendToPlayer(player, new CooldownSyncPacket(
                net.coms.ironspellabilityaddon.SpellLoadout.cooldownSnapshot(player)));
    }

    static void openData(ServerPlayer player, int mode) {
        SpellAbilityService.requestData(player, mode);
    }

    static void updateSlot(ServerPlayer player, int slot, String id, int level) {
        SpellAbilityService.setSlot(player, slot, id, level);
    }

    static void cast(ServerPlayer player, int slot) {
        SpellAbilityService.castSlot(player, slot);
    }

}
