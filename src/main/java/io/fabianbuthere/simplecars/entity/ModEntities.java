package io.fabianbuthere.simplecars.entity;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SimplecarsMod.MOD_ID);

    public static final RegistryObject<EntityType<BaseCarEntity>> BASE_CAR = ENTITY_TYPES
            .register("base_car", () -> EntityType.Builder.of(BaseCarEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).build("base_car"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
