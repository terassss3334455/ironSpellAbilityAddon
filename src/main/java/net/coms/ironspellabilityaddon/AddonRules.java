package net.coms.ironspellabilityaddon;

/** Pure rules shared by production packet handling and the offline contract tests. */
public final class AddonRules {
    public static final int SLOT_COUNT = 6;
    public static final int MAX_SPELL_ID_LENGTH = 256;
    public static final int MAX_SPELL_LEVEL = 100;
    public static final int MAX_COOLDOWN_TICKS = 72_000;

    private AddonRules() {}

    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    /** Empty ID plus level zero means clear; a spell assignment needs a positive level. */
    public static boolean isValidSlotUpdate(int slot, String spellId, int level) {
        if (!isValidSlot(slot) || spellId == null || spellId.length() > MAX_SPELL_ID_LENGTH
                || !spellId.equals(spellId.trim()) || level < 0 || level > MAX_SPELL_LEVEL) {
            return false;
        }
        return spellId.isEmpty() ? level == 0 : level > 0;
    }

    public static int remainingCooldownTicks(long expiresAt, long gameTime) {
        if (expiresAt <= gameTime) return 0;
        long remaining;
        try {
            remaining = Math.subtractExact(expiresAt, gameTime);
        } catch (ArithmeticException overflow) {
            return MAX_COOLDOWN_TICKS;
        }
        return (int) Math.min(MAX_COOLDOWN_TICKS, remaining);
    }

    public static long cooldownExpiry(long gameTime, int ticks) {
        if (ticks <= 0) return gameTime;
        long safeTicks = Math.min((long) MAX_COOLDOWN_TICKS, ticks);
        if (gameTime > Long.MAX_VALUE - safeTicks) return Long.MAX_VALUE;
        return gameTime + safeTicks;
    }

    /** Mirrors Minecraft Strength's +3 attack-damage-per-level effect for spell damage. */
    public static int strengthSpellDamageBonus(int amplifier) {
        if (amplifier < 0) return 0;
        long bonus = 3L * ((long) amplifier + 1L);
        return (int) Math.min(Integer.MAX_VALUE, bonus);
    }

    public static boolean shouldCommitCooldown(boolean spellEffectRan) {
        return spellEffectRan;
    }
}
