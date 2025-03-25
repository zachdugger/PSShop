package com.blissy.shop.util;

import com.blissy.shop.Shop;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A simplified handler for Pixelmon TMs and TRs without direct NMS imports
 */
public class SimplifiedTMHandler {

    /**
     * Create a TM ItemStack for the given move
     * @param moveName The move name
     * @return The ItemStack or null if creation failed
     */
    public static ItemStack createTM(String moveName) {
        return createTechnicalMove(true, moveName);
    }

    /**
     * Create a TR ItemStack for the given move
     * @param moveName The move name
     * @return The ItemStack or null if creation failed
     */
    public static ItemStack createTR(String moveName) {
        return createTechnicalMove(false, moveName);
    }

    /**
     * Create a technical move (TM or TR) ItemStack
     * @param isTM Whether this is a TM (true) or TR (false)
     * @param moveName The move name
     * @return The ItemStack or null if creation failed
     */
    private static ItemStack createTechnicalMove(boolean isTM, String moveName) {
        try {
            // Try to use reflection to get the Pixelmon item
            // But since this is just a simplified version, we'll skip that for now

            // Create a custom item to represent the TM/TR
            return createFallbackTMItem(isTM, moveName);

        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING,
                    "Failed to create " + (isTM ? "TM" : "TR") + " for " + moveName + ": " + e.getMessage(), e);
            return createFallbackTMItem(isTM, moveName);
        }
    }

    /**
     * Create a fallback ItemStack for a TM/TR
     * @param isTM Whether this is a TM (true) or TR (false)
     * @param moveName The move name
     * @return A custom ItemStack representing the TM/TR
     */
    private static ItemStack createFallbackTMItem(boolean isTM, String moveName) {
        // Use a music disc as fallback material
        ItemStack item = new ItemStack(Material.MUSIC_DISC_MALL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String formattedName = formatMoveName(moveName);
            meta.setDisplayName("§e" + (isTM ? "TM" : "TR") + ": " + formattedName);

            List<String> lore = new ArrayList<>();
            lore.add("§7Teaches a Pokémon the move");
            lore.add("§7" + formattedName);

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Format a move name to have proper capitalization
     * @param moveName The move name to format
     * @return The formatted move name
     */
    private static String formatMoveName(String moveName) {
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
}