package net.coms.ironspellabilityaddon.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class ClientState {
    public record SpellView(String id, String title, int level) {}
    public record SlotView(String id, String title, int level, int remaining) {
        public boolean empty() { return id == null || id.isEmpty(); }
    }

    private static List<SpellView> known = List.of();
    private static List<SlotView> slots = emptySlots();
    private static int mode = -1;
    private static String status = "";
    private static boolean enabled;

    private ClientState() {}

    public static void apply(CompoundTag data) {
        List<SpellView> nextKnown = new ArrayList<>();
        ListTag knownTag = data.getList("known", Tag.TAG_COMPOUND);
        for (int i = 0; i < knownTag.size(); i++) {
            CompoundTag row = knownTag.getCompound(i);
            String id = row.getString("id");
            if (!id.isEmpty()) nextKnown.add(new SpellView(
                    id, row.getString("name"), Math.max(1, row.getInt("level"))));
        }

        List<SlotView> nextSlots = new ArrayList<>();
        ListTag slotTag = data.getList("slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < 6; i++) {
            CompoundTag row = i < slotTag.size() ? slotTag.getCompound(i) : new CompoundTag();
            nextSlots.add(new SlotView(row.getString("id"), row.getString("name"),
                    Math.max(0, row.getInt("level")), Math.max(0, row.getInt("remaining"))));
        }
        known = List.copyOf(nextKnown);
        slots = List.copyOf(nextSlots);
        mode = data.getInt("mode");
        status = data.getString("status");
        enabled = data.getBoolean("enabled");
    }

    public static void applyCooldowns(CompoundTag data) {
        List<SlotView> next = new ArrayList<>(slots);
        for (int i = 0; i < 6; i++) {
            SlotView current = next.get(i);
            next.set(i, new SlotView(current.id(), current.title(), current.level(),
                    Math.max(0, data.getInt("slot_" + i))));
        }
        slots = List.copyOf(next);
    }

    public static void tick() {
        List<SlotView> next = new ArrayList<>(slots.size());
        for (SlotView slot : slots) {
            next.add(new SlotView(slot.id(), slot.title(), slot.level(),
                    Math.max(0, slot.remaining() - 1)));
        }
        slots = List.copyOf(next);
    }

    public static List<SpellView> known() { return known; }
    public static List<SlotView> slots() { return slots; }
    public static int mode() { return mode; }
    public static String status() { return status; }
    public static boolean enabled() { return enabled; }

    private static List<SlotView> emptySlots() {
        List<SlotView> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) result.add(new SlotView("", "", 0, 0));
        return List.copyOf(result);
    }
}
