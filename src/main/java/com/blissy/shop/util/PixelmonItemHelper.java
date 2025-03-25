package com.blissy.shop.util;

import com.blissy.shop.Shop;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * A handler for Pixelmon items using reflection instead of direct imports
 */
public class PixelmonItemHelper {

    // Reflection class paths
    private static final String REGISTRY_MANAGER_CLASS = "com.pixelmonmod.api.registry.RegistryManager";
    private static final String REGISTRY_VALUE_CLASS = "com.pixelmonmod.api.registry.RegistryValue";
    private static final String POKEBALL_REGISTRY_CLASS = "com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry";

    // Fallback materials for different types of items
    private static final Map<String, Material> FALLBACK_MATERIALS = new HashMap<>();

    static {
        // Initialize fallback materials
        FALLBACK_MATERIALS.put("poke_ball", Material.SNOWBALL);
        FALLBACK_MATERIALS.put("tm", Material.MUSIC_DISC_MALL);
        FALLBACK_MATERIALS.put("tr", Material.MUSIC_DISC_FAR);
        FALLBACK_MATERIALS.put("berry", Material.APPLE);
        FALLBACK_MATERIALS.put("evolution_stone", Material.EMERALD);
        FALLBACK_MATERIALS.put("held_item", Material.ARMOR_STAND);
        FALLBACK_MATERIALS.put("z_crystal", Material.PRISMARINE_CRYSTALS);
    }

    /**
     * Create a Pixelmon item using reflection and fallbacks
     * @param pixelmonId The Pixelmon item ID
     * @param displayName The name to display
     * @param description The item description
     * @return The created ItemStack
     */
    public static ItemStack createPixelmonItem(String pixelmonId, String displayName, List<String> description) {
        ItemStack result = null;

        // Normalize the ID
        String normalizedId = normalizeItemId(pixelmonId);

        // Determine the item type
        String itemType = determineItemType(normalizedId);

        // Try to create the specific item type
        if (itemType.equals("pokeball")) {
            result = createPokeballItem(normalizedId, displayName);
        } else if (itemType.equals("tm")) {
            String moveName = extractMoveNameFromId(normalizedId);
            if (moveName != null) {
                result = SimplifiedTMHandler.createTM(moveName);
            }
        } else if (itemType.equals("tr")) {
            String moveName = extractMoveNameFromId(normalizedId);
            if (moveName != null) {
                result = SimplifiedTMHandler.createTR(moveName);
            }
        } else {
            // For other items, try to use the registry reflection approach
            result = createGenericPixelmonItem(normalizedId);
        }

        // If all else fails, create a fallback item
        if (result == null) {
            result = createFallbackItem(itemType, displayName, description);
        } else {
            // If we have a result but need to customize it
            ItemMeta meta = result.getItemMeta();
            if (meta != null && displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);

                if (description != null && !description.isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String line : description) {
                        lore.add("§7" + line);
                    }
                    meta.setLore(lore);
                }

                result.setItemMeta(meta);
            }
        }

        return result;
    }

    /**
     * Normalize an item ID to ensure proper format
     */
    private static String normalizeItemId(String itemId) {
        if (itemId == null) return "";

        String normalized = itemId.toLowerCase().trim();
        if (!normalized.contains(":")) {
            normalized = "pixelmon:" + normalized;
        }

        return normalized;
    }

    /**
     * Determine the type of Pixelmon item from its ID
     */
    private static String determineItemType(String itemId) {
        String baseId = itemId.contains(":") ? itemId.split(":")[1] : itemId;

        if (baseId.endsWith("ball") || baseId.contains("_ball")) {
            return "pokeball";
        } else if (baseId.startsWith("tm") || baseId.startsWith("tm_")) {
            return "tm";
        } else if (baseId.startsWith("tr") || baseId.startsWith("tr_")) {
            return "tr";
        } else if (baseId.endsWith("_berry") || baseId.endsWith("berry")) {
            return "berry";
        } else if (baseId.endsWith("_stone") && !baseId.equals("everstone")) {
            return "evolution_stone";
        } else if (baseId.endsWith("ium_z") || baseId.endsWith("iumz")) {
            return "z_crystal";
        } else if (baseId.equals("choice_band") || baseId.equals("leftovers") ||
                baseId.equals("focus_sash") || baseId.equals("life_orb")) {
            return "held_item";
        } else {
            return "other";
        }
    }

    /**
     * Extract move name from a TM/TR ID
     */
    private static String extractMoveNameFromId(String itemId) {
        String baseId = itemId.contains(":") ? itemId.split(":")[1] : itemId;

        // Handle various TM/TR ID formats
        if (baseId.startsWith("tm_") || baseId.startsWith("tr_")) {
            // Format: tm_movename or tr_movename
            String[] parts = baseId.split("_", 2);
            if (parts.length > 1) {
                return parts[1];
            }
        } else if (baseId.matches("tm\\d+.*") || baseId.matches("tr\\d+.*")) {
            // Format: tm123_movename or tr45_movename
            int underscoreIndex = baseId.indexOf('_');
            if (underscoreIndex > 0) {
                return baseId.substring(underscoreIndex + 1);
            }
        }

        return null;
    }

    /**
     * Create a Pokeball item
     */
    private static ItemStack createPokeballItem(String itemId, String displayName) {
        try {
            String ballType = itemId.contains(":") ? itemId.split(":")[1] : itemId;

            // Try to use PokeBallRegistry via reflection
            Class<?> pokeballRegistryClass = Class.forName(POKEBALL_REGISTRY_CLASS);
            Method getPokeBallMethod = pokeballRegistryClass.getMethod("getPokeBall", String.class);

            Object registryValue = getPokeBallMethod.invoke(null, ballType);
            if (registryValue != null) {
                // Get the PokeBall object
                Class<?> registryValueClass = Class.forName(REGISTRY_VALUE_CLASS);
                Method getValueMethod = registryValueClass.getMethod("getValueUnsafe");
                Object pokeBall = getValueMethod.invoke(registryValue);

                if (pokeBall != null) {
                    // Get the item for this PokeBall
                    Method getItemMethod = pokeBall.getClass().getMethod("getItem");
                    Object nmsItem = getItemMethod.invoke(pokeBall);

                    if (nmsItem != null) {
                        // Create an NMS ItemStack
                        Class<?> nmsItemStackClass = Class.forName("net.minecraft.item.ItemStack");
                        Object nmsStack = nmsItemStackClass.getConstructor(Class.forName("net.minecraft.item.Item"))
                                .newInstance(nmsItem);

                        // Convert to Bukkit ItemStack
                        return convertNMSItemToBukkit(nmsStack);
                    }
                }
            }
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING, "Failed to create Pokeball item: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create a generic Pixelmon item using registry reflection
     */
    private static ItemStack createGenericPixelmonItem(String normalizedId) {
        try {
            // Access RegistryManager.get(Item.class, normalizedId)
            Class<?> registryManagerClass = Class.forName(REGISTRY_MANAGER_CLASS);
            Method getMethod = registryManagerClass.getMethod("get", Class.class, String.class);

            // Get the NMS Item class
            Class<?> nmsItemClass = Class.forName("net.minecraft.item.Item");

            // Get the registry value
            Object registryValue = getMethod.invoke(null, nmsItemClass, normalizedId);

            if (registryValue != null) {
                // Get the isInitialized method
                Class<?> registryValueClass = Class.forName(REGISTRY_VALUE_CLASS);
                Method isInitializedMethod = registryValueClass.getMethod("isInitialized");
                Boolean isInitialized = (Boolean) isInitializedMethod.invoke(registryValue);

                if (isInitialized) {
                    // Get the item
                    Method getValueMethod = registryValueClass.getMethod("getValueUnsafe");
                    Object nmsItem = getValueMethod.invoke(registryValue);

                    if (nmsItem != null) {
                        // Create an NMS ItemStack
                        Class<?> nmsItemStackClass = Class.forName("net.minecraft.item.ItemStack");
                        Object nmsStack = nmsItemStackClass.getConstructor(nmsItemClass)
                                .newInstance(nmsItem);

                        // Convert to Bukkit ItemStack
                        return convertNMSItemToBukkit(nmsStack);
                    }
                }
            }
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING, "Failed to create Pixelmon item: " + e.getMessage());
        }

        return null;
    }

    /**
     * Convert NMS ItemStack to Bukkit ItemStack using reflection
     */
    private static ItemStack convertNMSItemToBukkit(Object nmsStack) {
        try {
            // Get the CraftItemStack class
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack");

            // Get the asBukkitCopy method
            Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy",
                    Class.forName("net.minecraft.item.ItemStack"));

            // Invoke the method to convert
            Object bukkitStack = asBukkitCopyMethod.invoke(null, nmsStack);

            if (bukkitStack instanceof ItemStack) {
                return (ItemStack) bukkitStack;
            }
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING, "Failed to convert NMS item: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create a fallback item when Pixelmon item creation fails
     */
    private static ItemStack createFallbackItem(String itemType, String displayName, List<String> description) {
        // Get the appropriate fallback material
        Material material = FALLBACK_MATERIALS.getOrDefault(itemType, Material.STONE);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }

            // Set lore/description
            if (description != null && !description.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : description) {
                    lore.add("§7" + line);
                }
                lore.add("");
                lore.add("§8(Fallback item - Pixelmon item not available)");
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}