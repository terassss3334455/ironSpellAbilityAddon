package net.coms.ironspellabilityaddon;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpellLoadout {
    static final int SLOT_COUNT = AddonRules.SLOT_COUNT;
    private static final String LOADOUT_KEY = "loadout";
    private static final String COOLDOWNS_KEY = "cooldowns";

    record KnownSpell(String id, String title, int level) {}

    private SpellLoadout() {}

    static boolean canUse(Player player) {
        return AbilityApi.isAbilityEnabled(player, IronSpellAbilityAddon.ABILITY_ID);
    }

    static List<KnownSpell> knownSpells(ServerPlayer player) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        Map<String, KnownSpell> known = new HashMap<>();
        SpellSelectionManager selection = new SpellSelectionManager(player);
        for (SpellSelectionManager.SelectionOption option : selection.getAllSpells()) {
            SpellData data = option.spellData;
            if (data == null || data.equals(SpellData.EMPTY)) continue;
            AbstractSpell spell = data.getSpell();
            if (spell == null || spell.getSpellResource() == null || !spell.isEnabled()
                    || !magicData.getSyncedData().isSpellLearned(spell)) continue;
            int level = Math.max(spell.getLevelFor(data.getLevel(), player),
                    highestCarriedLevel(player, spell));
            KnownSpell candidate = new KnownSpell(spell.getSpellId(),
                    spell.getDisplayName(player).getString(), level);
            known.merge(candidate.id(), candidate,
                    (first, second) -> first.level() >= second.level() ? first : second);
        }
        return known.values().stream()
                .sorted(Comparator.comparing(KnownSpell::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(KnownSpell::id))
                .limit(512)
                .toList();
    }

    private static int highestCarriedLevel(ServerPlayer player, AbstractSpell spell) {
        int[] highest = {Math.max(1, spell.getMinLevel())};
        for (ItemStack stack : player.getInventory().items) scanContainer(stack, spell, player, highest);
        for (ItemStack stack : player.getInventory().armor) scanContainer(stack, spell, player, highest);
        for (ItemStack stack : player.getInventory().offhand) scanContainer(stack, spell, player, highest);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findCurios(ISpellContainer::isSpellContainer)
                        .forEach(result -> scanContainer(result.stack(), spell, player, highest)));
        return Math.max(1, highest[0]);
    }

    private static void scanContainer(ItemStack stack, AbstractSpell spell,
                                      ServerPlayer player, int[] highest) {
        if (!ISpellContainer.isSpellContainer(stack)) return;
        try {
            ISpellContainer container = ISpellContainer.get(stack);
            if (container == null) return;
            for (SpellSlot slot : container.getActiveSpells()) {
                SpellData data = slot.spellData();
                if (data != null && data.getSpell().getSpellResource().equals(spell.getSpellResource())) {
                    highest[0] = Math.max(highest[0], spell.getLevelFor(data.getLevel(), player));
                }
            }
        } catch (RuntimeException ignored) {
            // Invalid/old spell-container data must not prevent opening the ability UI.
        }
    }

    static CompoundTag snapshot(ServerPlayer player, int mode, String status) {
        CompoundTag result = new CompoundTag();
        result.putInt("mode", mode);
        result.putString("status", status == null ? "" : status);
        result.putBoolean("enabled", canUse(player));

        ListTag knownTag = new ListTag();
        Map<String, KnownSpell> known = new HashMap<>();
        if (canUse(player)) {
            for (KnownSpell knownSpell : knownSpells(player)) {
                CompoundTag row = new CompoundTag();
                row.putString("id", knownSpell.id());
                row.putString("name", knownSpell.title());
                row.putInt("level", knownSpell.level());
                knownTag.add(row);
                known.put(knownSpell.id(), knownSpell);
            }
        }
        result.put("known", knownTag);

        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        CompoundTag loadout = data.getCompound(LOADOUT_KEY);
        ListTag slots = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = loadout.getString("spell_" + i);
            KnownSpell knownSpell = knownSpell(player, id);
            CompoundTag row = new CompoundTag();
            row.putInt("index", i);
            if (knownSpell != null) {
                row.putString("id", id);
                row.putString("name", knownSpell.title());
                row.putInt("level", Math.max(knownSpell.level(), loadout.getInt("level_" + i)));
                row.putInt("remaining", cooldownRemaining(player, data, id));
            } else {
                row.putString("id", "");
                row.putString("name", "");
                row.putInt("level", 0);
                row.putInt("remaining", 0);
            }
            slots.add(row);
        }
        result.put("slots", slots);
        return result;
    }

    static String setSlot(ServerPlayer player, int index, String id, int requestedLevel) {
        if (index < 0 || index >= SLOT_COUNT || !canUse(player)) return "disabled";
        String safeId = id == null ? "" : id.trim();
        KnownSpell selectedSpell = null;
        if (!safeId.isEmpty()) {
            for (KnownSpell known : knownSpells(player)) {
                if (known.id().equals(safeId) && known.level() == requestedLevel) {
                    selectedSpell = known;
                    break;
                }
            }
            if (selectedSpell == null) return "spell_unavailable";
        }

        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        CompoundTag loadout = data.getCompound(LOADOUT_KEY);
        String oldId = loadout.getString("spell_" + index);
        int oldLevel = Math.max(1, loadout.getInt("level_" + index));
        if (!safeId.isEmpty() && safeId.equals(oldId) && selectedSpell.level() == oldLevel) return "slot_saved";

        if (selectedSpell == null) {
            loadout.remove("spell_" + index);
            loadout.remove("level_" + index);
            data.put(LOADOUT_KEY, loadout);
            return AbilityApi.setAbilityData(player, IronSpellAbilityAddon.ABILITY_ID, data)
                    ? "slot_removed" : "spell_unavailable";
        }

        loadout.putString("spell_" + index, selectedSpell.id());
        loadout.putInt("level_" + index, selectedSpell.level());
        data.put(LOADOUT_KEY, loadout);
        return AbilityApi.setAbilityData(player, IronSpellAbilityAddon.ABILITY_ID, data)
                ? "slot_saved" : "spell_unavailable";
    }

    static String spellId(ServerPlayer player, int index) {
        if (index < 0 || index >= SLOT_COUNT || !canUse(player)) return "";
        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        String id = data.getCompound(LOADOUT_KEY).getString("spell_" + index);
        return knownSpell(player, id) == null ? "" : id;
    }

    static int spellLevel(ServerPlayer player, int index, AbstractSpell spell) {
        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        int stored = data.getCompound(LOADOUT_KEY).getInt("level_" + index);
        int carried = highestCarriedLevel(player, spell);
        return spell.getLevelFor(Math.max(Math.max(1, spell.getMinLevel()), Math.max(stored, carried)), player);
    }

    static AbstractSpell resolveKnownSpell(ServerPlayer player, String id) {
        if (id == null || id.length() > 256) return null;
        ResourceLocation resource = ResourceLocation.tryParse(id);
        if (resource == null) return null;
        AbstractSpell spell = SpellRegistry.getSpell(resource);
        if (spell == null || spell.getSpellResource() == null
                || !spell.getSpellId().equals(id) || !spell.isEnabled()) return null;
        return MagicData.getPlayerMagicData(player).getSyncedData().isSpellLearned(spell)
                ? spell : null;
    }

    private static KnownSpell knownSpell(ServerPlayer player, String id) {
        if (id == null || id.isEmpty()) return null;
        AbstractSpell spell = resolveKnownSpell(player, id);
        if (spell == null) return null;
        return new KnownSpell(id, spell.getDisplayName(player).getString(),
                Math.max(spell.getLevelFor(spell.getMinLevel(), player), highestCarriedLevel(player, spell)));
    }

    static int cooldownRemaining(ServerPlayer player, String id) {
        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        return cooldownRemaining(player, data, id);
    }

    private static int cooldownRemaining(ServerPlayer player, CompoundTag data, String id) {
        long until = data.getCompound(COOLDOWNS_KEY).getLong(id);
        return AddonRules.remainingCooldownTicks(until, clock(player));
    }

    static void putCooldown(ServerPlayer player, String id, int ticks) {
        CompoundTag data = AbilityApi.getAbilityData(player, IronSpellAbilityAddon.ABILITY_ID);
        CompoundTag cooldowns = data.getCompound(COOLDOWNS_KEY);
        if (ticks <= 0) cooldowns.remove(id);
        else cooldowns.putLong(id, AddonRules.cooldownExpiry(clock(player), ticks));
        data.put(COOLDOWNS_KEY, cooldowns);
        AbilityApi.setAbilityData(player, IronSpellAbilityAddon.ABILITY_ID, data);
    }

    private static long clock(ServerPlayer player) {
        return player.getServer().overworld().getGameTime();
    }

    public static CompoundTag cooldownSnapshot(ServerPlayer player) {
        CompoundTag result = new CompoundTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = spellId(player, i);
            if (!id.isEmpty()) result.putInt("slot_" + i, cooldownRemaining(player, id));
        }
        return result;
    }

    static boolean hasAnyCooldown(ServerPlayer player) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = spellId(player, i);
            if (!id.isEmpty() && cooldownRemaining(player, id) > 0) return true;
        }
        return false;
    }
}
