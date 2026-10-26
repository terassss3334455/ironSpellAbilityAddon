package net.coms.ironspellabilityaddon;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.ironspellabilityaddon.AddonRules;
import net.coms.abilitysgorwmod.api.event.AbilityApiEvents;
import net.coms.ironspellabilityaddon.api.SpellAbilityEvents;
import net.coms.ironspellabilityaddon.network.AddonNetwork;
import net.coms.ironspellabilityaddon.network.SpellDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellAbilityService {
    private static final Map<UUID, PendingCast> PENDING = new ConcurrentHashMap<>();

    private static final class PendingCast {
        final String spellId;
        final int spellLevel;
        final int cooldownTicks;
        volatile boolean effectRan;

        PendingCast(String spellId, int spellLevel, int cooldownTicks) {
            this.spellId = spellId;
            this.spellLevel = spellLevel;
            this.cooldownTicks = cooldownTicks;
        }
    }

    public static void requestData(ServerPlayer player, int mode) {
        if (!SpellLoadout.canUse(player)) {
            if (mode >= 0) {
                player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.disabled")
                        .withStyle(ChatFormatting.RED), true);
            } else {
                AddonNetwork.sendToPlayer(player,
                        new SpellDataPacket(SpellLoadout.snapshot(player, -1, "")));
            }
            return;
        }
        AddonNetwork.sendToPlayer(player,
                new SpellDataPacket(SpellLoadout.snapshot(player, mode, "")));
    }

    public static void setSlot(ServerPlayer player, int index, String id, int level) {
        String result = SpellLoadout.setSlot(player, index, id, level);
        String status = "message.ironspellabilityaddon." + result;
        AddonNetwork.sendToPlayer(player,
                new SpellDataPacket(SpellLoadout.snapshot(player, 0, status)));
    }

    public static void castSlot(ServerPlayer player, int index) {
        if (!SpellLoadout.canUse(player)) {
            player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.disabled")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (index < 0 || index >= SpellLoadout.SLOT_COUNT) return;
        if (PENDING.containsKey(player.getUUID()) || MagicData.getPlayerMagicData(player).isCasting()) {
            player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.already_casting")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        String id = SpellLoadout.spellId(player, index);
        AbstractSpell spell = SpellLoadout.resolveKnownSpell(player, id);
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.spell_unavailable")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        int remaining = SpellLoadout.cooldownRemaining(player, id);
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.ironspellabilityaddon.cooldown", String.format("%.1f", remaining / 20.0F))
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            player.displayClientMessage(Component.translatable(
                    "message.ironspellabilityaddon.iron_cooldown").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        int level = SpellLoadout.spellLevel(player, index, spell);
        double reduction = player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION.get());
        int baseCooldown = (int) (spell.getSpellCooldown() * (2.0 - Utils.softCapFormula(reduction)));
        SpellAbilityEvents.Cooldown cooldownEvent =
                new SpellAbilityEvents.Cooldown(player, spell, Math.max(1, baseCooldown));
        MinecraftForge.EVENT_BUS.post(cooldownEvent);
        int cooldownTicks = cooldownEvent.getCooldownTicks();

        // COMMAND is Iron's own non-mana-consuming cast source. It retains spell checks,
        // server pre-cast hooks, normal spell execution, interruption, and Iron cooldowns.
        boolean started = spell.attemptInitiateCast(ItemStack.EMPTY, level, player.level(), player,
                CastSource.COMMAND, true, SpellSelectionManager.MAINHAND);
        if (!started) {
            player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.cast_rejected")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        PENDING.put(player.getUUID(), new PendingCast(id, level, cooldownTicks));
        player.displayClientMessage(Component.translatable("message.ironspellabilityaddon.cast_started")
                .withStyle(ChatFormatting.GOLD), true);
    }

    @SubscribeEvent
    public void onIronSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getCastSource() != CastSource.COMMAND) return;
        PendingCast pending = PENDING.get(player.getUUID());
        if (pending != null && pending.spellId.equals(event.getSpellId())) pending.effectRan = true;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        PendingCast pending = PENDING.get(player.getUUID());
        if (pending != null) {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (magicData.isCasting() && pending.spellId.equals(magicData.getCastingSpellId())
                    && magicData.getCastType() == CastType.LONG
                    && magicData.getCastDurationRemaining() <= 0) {
                AbstractSpell spell = SpellLoadout.resolveKnownSpell(player, pending.spellId);
                if (spell != null) {
                    spell.castSpell(player.level(), pending.spellLevel, player,
                            magicData.getCastSource(), true);
                    spell.onServerCastComplete(player.level(), pending.spellLevel, player, magicData, false);
                } else {
                    magicData.resetCastingState();
                }
            }

            if (!magicData.isCasting()
                    || !pending.spellId.equals(magicData.getCastingSpellId())) {
                PENDING.remove(player.getUUID(), pending);
                if (AddonRules.shouldCommitCooldown(pending.effectRan)) {
                    SpellLoadout.putCooldown(player, pending.spellId, pending.cooldownTicks);
                    AddonNetwork.sendCooldowns(player);
                }
            }
        }

        if (player.tickCount % 20 == 0 && SpellLoadout.hasAnyCooldown(player)) {
            AddonNetwork.sendCooldowns(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onAbilityToggle(AbilityApiEvents.AbilityToggledEvent event) {
        if (IronSpellAbilityAddon.ABILITY_ID.equals(event.getAbilityId())
                && event.getPlayer() instanceof ServerPlayer player) {
            if (!event.isActive()) AddonNetwork.sendCooldowns(player);
            AddonNetwork.sendToPlayer(player,
                    new SpellDataPacket(SpellLoadout.snapshot(player, -1, "")));
        }
    }

    @SubscribeEvent
    public void onAbilityRemoved(AbilityApiEvents.AbilityRemovedEvent event) {
        if (IronSpellAbilityAddon.ABILITY_ID.equals(event.getAbilityId())
                && event.getPlayer() instanceof ServerPlayer player) {
            AddonNetwork.sendCooldowns(player);
            AddonNetwork.sendToPlayer(player,
                    new SpellDataPacket(SpellLoadout.snapshot(player, -1, "")));
        }
    }
}
