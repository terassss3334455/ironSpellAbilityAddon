package net.coms.ironspellabilityaddon;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.coms.ironspellabilityaddon.AddonRules;
import net.coms.ironspellabilityaddon.api.SpellAbilityEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Bridges the Curios combat charm's vanilla Strength effect to Iron spell damage. */
public final class SpellDamageHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSpellDamage(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        SpellDamageSource source = event.getSpellDamageSource();
        Entity owner = source.getEntity();
        if (!(owner instanceof Player caster)) return;

        float base = event.getAmount();
        float adjusted = base;
        MobEffectInstance strength = caster.getEffect(MobEffects.DAMAGE_BOOST);
        if (strength != null) {
            // Minecraft Strength adds three attack damage per level. Apply that same flat
            // bonus to spell damage, which otherwise does not read ATTACK_DAMAGE.
            adjusted += AddonRules.strengthSpellDamageBonus(strength.getAmplifier());
        }

        SpellAbilityEvents.SpellDamage extension = new SpellAbilityEvents.SpellDamage(
                caster, event.getEntity(), source.spell(), base, adjusted);
        MinecraftForge.EVENT_BUS.post(extension);
        event.setAmount(extension.getDamage());
    }
}
