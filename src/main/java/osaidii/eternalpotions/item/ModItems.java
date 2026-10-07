package osaidii.eternalpotions.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import osaidii.eternalpotions.EternalPotions;

import java.util.function.Function;

public class ModItems {

    public static final Item ETERNAL_SHARD = register(
            "eternal_shard",
            Item::new,
            new Item.Properties()
    );

    public static final Item ETERNAL_POTION = register(
            "eternal_potion",
            EternalPotionItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties settings) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, name)
        );
        Item item = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        EternalPotions.LOGGER.info("Registering Eternal Potions items");
    }
}