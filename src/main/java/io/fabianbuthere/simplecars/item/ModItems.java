package io.fabianbuthere.simplecars.item;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.item.custom.AbstractCarPartItem;
import io.fabianbuthere.simplecars.item.custom.IronEngineItem;
import io.fabianbuthere.simplecars.item.custom.SteelFrameItem;
import io.fabianbuthere.simplecars.item.custom.BasicFuelTankItem;
import io.fabianbuthere.simplecars.item.custom.StandardWheelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SimplecarsMod.MOD_ID);

    public static final RegistryObject<Item> IRON_ENGINE = ITEMS.register(
            "iron_engine", () -> new IronEngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> STEEL_FRAME = ITEMS.register(
            "steel_frame", () -> new SteelFrameItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BASIC_FUEL_TANK = ITEMS.register(
            "basic_fuel_tank", () -> new BasicFuelTankItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> STANDARD_WHEEL = ITEMS.register(
            "standard_wheel", () -> new StandardWheelItem(new Item.Properties().stacksTo(1)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
