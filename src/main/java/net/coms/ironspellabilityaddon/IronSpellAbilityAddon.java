package net.coms.ironspellabilityaddon;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.ironspellabilityaddon.network.AddonNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(IronSpellAbilityAddon.MODID)
public final class IronSpellAbilityAddon {
    public static final String MODID = "ironspellabilityaddon";
    public static final String ABILITY_ID = MODID + ":spellcaster";

    public IronSpellAbilityAddon() {
        AbilityApi.registerAbility(new AbilityDefinition(
                ABILITY_ID,
                "魔術使い",
                "Iron's Spells 'n Spellbooksの習得済み魔術を6枠に登録し、魔力消費なしで発動する。発動後はクールダウンがある。",
                true,
                true
        ));
        AddonNetwork.register();
        MinecraftForge.EVENT_BUS.register(new SpellAbilityService());
        MinecraftForge.EVENT_BUS.register(new SpellDamageHandler());
    }
}
