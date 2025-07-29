package io.fabianbuthere.simplecars.event;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.ModEntities;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimplecarsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BASE_CAR.get(), BaseCarEntity.createAttributes().build());
    }
}
