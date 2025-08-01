package cjs.DE_plugin.settings.apply;

import cjs.DE_plugin.DE_plugin;
import cjs.DE_plugin.settings.SettingsManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class RecipeListener implements Listener {

    private final SettingsManager sm;

    public RecipeListener(DE_plugin plugin) {
        this.sm = plugin.getSettingsManager();
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!sm.getBoolean(SettingsManager.CUSTOM_TEMPLATE_RECIPE_ENABLED)) {
            return;
        }

        SmithingInventory inventory = event.getInventory();
        ItemStack baseItem = inventory.getItem(1);
        ItemStack addition = inventory.getItem(2);
        ItemStack result = event.getResult();

        if (result != null && baseItem != null && baseItem.getType().name().endsWith("_SMITHING_TEMPLATE") && addition != null && addition.getType() == Material.NETHERITE_INGOT) {
            result.setAmount(1);
        }
    }
}