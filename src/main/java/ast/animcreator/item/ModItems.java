package ast.animcreator.item;

import ast.animcreator.AnimCreator;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {

    public final static Item RUBY = registerItem("ruby", Item::new, new Item.Settings());
    public final static Item RAW_RUBY = registerItem("raw_ruby", Item::new, new Item.Settings());

    public static Item registerItem(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tutorial", path));
        return Items.register(registryKey, factory, settings);
    }

    private static void addItemsToIngredientItemGroup(FabricItemGroupEntries entries)
    {
        entries.add(RUBY);
        entries.add(RAW_RUBY);
    }

    public static void registerModItems()
    {
        AnimCreator.LOGGER.info("Registering mod items for " + AnimCreator.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemsToIngredientItemGroup);
    }
}
