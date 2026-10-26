package net.coms.ironspellabilityaddon.api;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/** Extension points for other addons that want to tune the spell ability. */
public final class SpellAbilityEvents {
    private SpellAbilityEvents() {}

    public static final class Cooldown extends Event {
        private final ServerPlayer player;
        private final AbstractSpell spell;
        private final int baseTicks;
        private int cooldownTicks;

        public Cooldown(ServerPlayer player, AbstractSpell spell, int baseTicks) {
            this.player = player;
            this.spell = spell;
            this.baseTicks = baseTicks;
            this.cooldownTicks = baseTicks;
        }

        public ServerPlayer getPlayer() { return player; }
        public AbstractSpell getSpell() { return spell; }
        public int getBaseTicks() { return baseTicks; }
        public int getCooldownTicks() { return cooldownTicks; }

        public void setCooldownTicks(int value) {
            cooldownTicks = Math.max(1, Math.min(72000, value));
        }

        public void multiplyCooldown(double factor) {
            if (!Double.isFinite(factor)) return;
            setCooldownTicks((int) Math.round(cooldownTicks * Math.max(0.01, Math.min(100.0, factor))));
        }
    }

    public static final class SpellDamage extends Event {
        private final Player caster;
        private final LivingEntity target;
        private final AbstractSpell spell;
        private final float baseDamage;
        private float damage;

        public SpellDamage(Player caster, LivingEntity target, AbstractSpell spell,
                           float baseDamage, float damage) {
            this.caster = caster;
            this.target = target;
            this.spell = spell;
            this.baseDamage = baseDamage;
            this.damage = damage;
        }

        public Player getCaster() { return caster; }
        public LivingEntity getTarget() { return target; }
        public AbstractSpell getSpell() { return spell; }
        public float getBaseDamage() { return baseDamage; }
        public float getDamage() { return damage; }
        public void setDamage(float value) {
            damage = Float.isFinite(value) ? Math.max(0, Math.min(1_000_000, value)) : baseDamage;
        }
        public void addDamage(float value) { setDamage(damage + value); }
        public void multiplyDamage(float factor) {
            if (Float.isFinite(factor)) setDamage((float) (damage * Math.max(0, Math.min(1000, factor))));
        }
    }
}
