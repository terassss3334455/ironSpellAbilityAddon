package net.coms.ironspellabilityaddon.client;

import net.coms.ironspellabilityaddon.IronSpellAbilityAddon;
import net.coms.ironspellabilityaddon.network.AddonNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = IronSpellAbilityAddon.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ClientKeys {
    public static final KeyMapping CONFIGURE = new KeyMapping(
            "key.ironspellabilityaddon.configure", GLFW.GLFW_KEY_J,
            "key.categories.ironspellabilityaddon");
    public static final KeyMapping CAST = new KeyMapping(
            "key.ironspellabilityaddon.cast", GLFW.GLFW_KEY_L,
            "key.categories.ironspellabilityaddon");

    private ClientKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CONFIGURE);
        event.register(CAST);
    }
}
