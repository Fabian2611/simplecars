package io.fabianbuthere.simplecars.item.custom;

import io.fabianbuthere.simplecars.SimplecarsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/// Abstract superclass for all car part items.
///
/// I have decided to branch this class into more abstract classes for the different types of car parts,
/// so that custom ticking can be implemented in the future.
public abstract class AbstractCarPartItem extends Item {
    public AbstractCarPartItem(Properties pProperties) {
        super(pProperties);
    }

    /// The part's weight in kilograms.
    public abstract double getWeight();
    /// The part's type
    public abstract CarPartType getPartType();

    public CompoundTag serializeToTag() {
        CompoundTag tag = new CompoundTag();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null) {
            tag.putString("item_id", id.toString());
        }
        return tag;
    }

    @SuppressWarnings("removal")
    public static Item getItemFromTag(CompoundTag tag) {
        if (tag.contains("item_id")) {
            ResourceLocation id = new ResourceLocation(tag.getString("item_id"));
            return ForgeRegistries.ITEMS.getValue(id);
        }
        return null;
    }

    @SuppressWarnings("removal")
    public static <T extends Item> T getItemByDescriptionId(String descriptionId, Class<T> clazz) {
        String[] parts = descriptionId.split("\\.");
        if (parts.length < 3 || !("item".equals(parts[0]))) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[1], parts[2]));
        if (clazz.isInstance(item)) {
            return clazz.cast(item);
        }
        return null;
    }
}
