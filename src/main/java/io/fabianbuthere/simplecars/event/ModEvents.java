package io.fabianbuthere.simplecars.event;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.command.CarConstantsCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimplecarsMod.MOD_ID)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CarConstantsCommand.register(event.getDispatcher());
    }
}
