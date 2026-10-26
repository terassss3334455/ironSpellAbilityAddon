package net.coms.ironspellabilityaddon;

/** Dependency-free executable contract tests; run with ./gradlew verifyAddon. */
public final class AddonRulesContractTest {
    private static int checks;

    private AddonRulesContractTest() {}

    private static void expect(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError("Contract failed: " + label);
    }

    public static void main(String[] args) {
        for (int slot = 0; slot < AddonRules.SLOT_COUNT; slot++) {
            expect(AddonRules.isValidSlot(slot), "six-slot range includes " + slot);
        }
        expect(!AddonRules.isValidSlot(-1), "negative slot rejected");
        expect(!AddonRules.isValidSlot(6), "slot seven rejected");

        expect(AddonRules.isValidSlotUpdate(0, "", 0), "empty slot clear accepted");
        expect(AddonRules.isValidSlotUpdate(5, "irons_spellbooks:fireball", 1), "valid learned spell payload shape accepted");
        expect(!AddonRules.isValidSlotUpdate(6, "irons_spellbooks:fireball", 1), "out-of-range slot update rejected");
        expect(!AddonRules.isValidSlotUpdate(0, null, 0), "null spell id rejected");
        expect(!AddonRules.isValidSlotUpdate(0, "", 1), "clear must not carry a spell level");
        expect(!AddonRules.isValidSlotUpdate(0, "irons_spellbooks:fireball", 0), "spell assignment requires positive level");
        expect(!AddonRules.isValidSlotUpdate(0, "x".repeat(AddonRules.MAX_SPELL_ID_LENGTH + 1), 1), "oversized spell id rejected");
        expect(!AddonRules.isValidSlotUpdate(0, "irons_spellbooks:fireball", AddonRules.MAX_SPELL_LEVEL + 1), "excessive level rejected");
        expect(!AddonRules.isValidSlotUpdate(0, " irons_spellbooks:fireball", 1), "padded spell id rejected");

        expect(AddonRules.remainingCooldownTicks(120, 100) == 20, "cooldown ticks count down from world time");
        expect(AddonRules.remainingCooldownTicks(100, 100) == 0, "cooldown expires at deadline");
        expect(AddonRules.remainingCooldownTicks(99, 100) == 0, "expired cooldown clamps to zero");
        expect(AddonRules.remainingCooldownTicks(Long.MAX_VALUE, 0) == AddonRules.MAX_COOLDOWN_TICKS, "remaining cooldown is bounded");
        expect(AddonRules.cooldownExpiry(100, 20) == 120, "cooldown deadline uses world time");
        expect(AddonRules.cooldownExpiry(100, AddonRules.MAX_COOLDOWN_TICKS + 1) == 100L + AddonRules.MAX_COOLDOWN_TICKS, "stored cooldown is capped");
        expect(AddonRules.cooldownExpiry(Long.MAX_VALUE - 1, 10) == Long.MAX_VALUE, "deadline addition saturates safely");
        expect(AddonRules.cooldownExpiry(100, 0) == 100, "nonpositive cooldown does not extend deadline");

        expect(AddonRules.strengthSpellDamageBonus(-1) == 0, "no Strength amplifier has no bonus");
        expect(AddonRules.strengthSpellDamageBonus(0) == 3, "Strength I adds three spell damage");
        expect(AddonRules.strengthSpellDamageBonus(1) == 6, "Strength II adds six spell damage");
        expect(AddonRules.strengthSpellDamageBonus(Integer.MAX_VALUE) == Integer.MAX_VALUE, "Strength bonus saturates safely");
        expect(AddonRules.shouldCommitCooldown(true), "successful spell effect commits cooldown");
        expect(!AddonRules.shouldCommitCooldown(false), "rejected or interrupted spell does not commit cooldown");

        System.out.println("Addon contract checks passed: " + checks);
    }
}
