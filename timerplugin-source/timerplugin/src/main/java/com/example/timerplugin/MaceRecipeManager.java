package com.example.timerplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

/**
 * מסיר את מתכון היצירה המקורי של המייס (Mace) ורושם מתכון חדש
 * שדורש: Heavy Core + Breeze Rod + תפוח מכושף (Enchanted Golden Apple / "נוצ' אפל").
 * הפעולה מתבצעת ב-ServerLoadEvent כדי לוודא שכל המתכונים המקוריים כבר נטענו.
 */
public class MaceRecipeManager implements Listener {

    private final JavaPlugin plugin;

    public MaceRecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        applyCustomRecipe();
    }

    public void applyCustomRecipe() {
        // מסירים כל מתכון קיים שתוצאתו מייס (כולל המתכון המקורי של ה-Smithing Table)
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe.getResult().getType() == Material.MACE) {
                iterator.remove();
            }
        }

        // רושמים מתכון חדש בשולחן היצירה: Heavy Core + Breeze Rod + תפוח מכושף
        NamespacedKey key = new NamespacedKey(plugin, "custom_mace");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.MACE));
        recipe.addIngredient(Material.HEAVY_CORE);
        recipe.addIngredient(Material.BREEZE_ROD);
        recipe.addIngredient(Material.ENCHANTED_GOLDEN_APPLE);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("מתכון מותאם אישית למייס נרשם בהצלחה (Heavy Core + Breeze Rod + נוצ' אפל).");
    }
}
