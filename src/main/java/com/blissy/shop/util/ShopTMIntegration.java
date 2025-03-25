package com.blissy.shop.util;

import com.blissy.shop.Shop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility for adding TMs and TRs to the shop
 */
public class ShopTMIntegration {

    private final Shop plugin;

    public ShopTMIntegration(Shop plugin) {
        this.plugin = plugin;
    }

    /**
     * Add TMs and TRs to the shop using a simplified approach
     */
    public void addTMsAndTRs() {
        plugin.getLogger().info("Starting to add TMs and TRs to the shop...");

        try {
            // Create a TMs category if it doesn't exist
            addTMCategory();

            // Add some common TMs
            addCommonTMs();

            plugin.getLogger().info("Successfully added common TMs to the shop");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding TMs and TRs", e);
        }
    }

    /**
     * Add a TMs category to the shop
     */
    private void addTMCategory() {
        File categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        try {
            // Load categories file
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoriesFile);

            // Check if TMs category exists
            if (!config.isConfigurationSection("categories.tms")) {
                // Create the category
                config.createSection("categories.tms");
                config.set("categories.tms.name", "Technical Machines");
                config.set("categories.tms.icon", "MUSIC_DISC_MALL");
                config.set("categories.tms.pixelmon_icon", false);

                List<String> description = new ArrayList<>();
                description.add("Technical Machines for");
                description.add("teaching moves to Pokémon.");
                config.set("categories.tms.description", description);

                config.set("categories.tms.slot", 21);
                config.set("categories.tms.items", new ArrayList<>());

                // Save the updated config
                config.save(categoriesFile);

                plugin.getLogger().info("Created TMs category");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create TMs category", e);
        }
    }

    /**
     * Add some common TMs to the shop
     */
    private void addCommonTMs() {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        try {
            // Load items file
            FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

            // Ensure items section exists
            if (!config.isConfigurationSection("items")) {
                config.createSection("items");
            }

            // List of common TMs to add
            Map<String, String> commonTMs = new HashMap<>();
            commonTMs.put("thunderbolt", "Electric");
            commonTMs.put("flamethrower", "Fire");
            commonTMs.put("ice_beam", "Ice");
            commonTMs.put("earthquake", "Ground");
            commonTMs.put("psychic", "Psychic");
            commonTMs.put("shadow_ball", "Ghost");
            commonTMs.put("surf", "Water");
            commonTMs.put("solar_beam", "Grass");

            // Category items list
            List<String> categoryItems = new ArrayList<>();

            // Add each TM
            int slot = 10;
            for (Map.Entry<String, String> entry : commonTMs.entrySet()) {
                String moveName = entry.getKey();
                String moveType = entry.getValue();
                String itemId = "tm_" + moveName;

                // Skip if item already exists
                if (config.isConfigurationSection("items." + itemId)) {
                    categoryItems.add(itemId);
                    continue;
                }

                // Create item entry
                config.createSection("items." + itemId);

                // Set display name with appropriate color
                String colorCode = getTMColorCode(moveType);
                config.set("items." + itemId + ".name", colorCode + "TM: " + formatMoveName(moveName));

                // Set material (fallback)
                config.set("items." + itemId + ".material", "MUSIC_DISC_MALL");
                config.set("items." + itemId + ".amount", 1);

                // Set description
                List<String> description = new ArrayList<>();
                description.add(moveType + " type move");
                description.add("Teaches a Pokémon");
                description.add("the " + formatMoveName(moveName) + " move");
                config.set("items." + itemId + ".description", description);

                // Set price based on type
                int price = getTMPrice(moveType);
                config.set("items." + itemId + ".currency", "coins");
                config.set("items." + itemId + ".price", price);

                // Set slot
                config.set("items." + itemId + ".slot", slot++);

                // Set Pixelmon data
                config.set("items." + itemId + ".pixelmon_id", "tm");

                // Set NBT data
                config.createSection("items." + itemId + ".pixelmon_nbt");
                config.set("items." + itemId + ".pixelmon_nbt.move", moveName);

                // Add to category items
                categoryItems.add(itemId);

                plugin.getLogger().info("Added TM: " + moveName);
            }

            // Save the updated config
            config.save(itemsFile);

            // Update the TMs category with the items
            updateTMCategory(categoryItems);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add common TMs", e);
        }
    }

    /**
     * Update the TMs category with items
     */
    private void updateTMCategory(List<String> items) {
        File categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        try {
            // Load categories file
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoriesFile);

            // Update items list
            config.set("categories.tms.items", items);

            // Save the updated config
            config.save(categoriesFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update TMs category", e);
        }
    }

    /**
     * Format a move name with proper capitalization
     */
    private String formatMoveName(String moveName) {
        if (moveName == null || moveName.isEmpty()) {
            return "";
        }

        String[] words = moveName.split("_|\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Get color code for TM based on move type
     */
    private String getTMColorCode(String moveType) {
        switch (moveType.toLowerCase()) {
            case "normal": return "&f"; // White
            case "fire": return "&c"; // Red
            case "water": return "&9"; // Blue
            case "electric": return "&e"; // Yellow
            case "grass": return "&a"; // Green
            case "ice": return "&b"; // Aqua
            case "fighting": return "&4"; // Dark Red
            case "poison": return "&5"; // Purple
            case "ground": return "&6"; // Gold
            case "flying": return "&f"; // White
            case "psychic": return "&d"; // Light Purple
            case "bug": return "&2"; // Dark Green
            case "rock": return "&8"; // Dark Gray
            case "ghost": return "&5"; // Purple
            case "dragon": return "&1"; // Dark Blue
            case "dark": return "&8"; // Dark Gray
            case "steel": return "&7"; // Gray
            case "fairy": return "&d"; // Light Purple
            default: return "&f"; // White
        }
    }

    /**
     * Get price for TM based on move type
     */
    private int getTMPrice(String moveType) {
        switch (moveType.toLowerCase()) {
            case "dragon":
            case "fairy":
            case "steel":
            case "fire":
            case "water":
            case "electric":
            case "psychic":
            case "ghost":
            case "dark":
            case "ice":
            case "fighting":
                return 3000;
            default:
                return 15000;
        }
    }
}