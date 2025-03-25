package com.blissy.shop.models;

import com.blissy.shop.Shop;
import com.blissy.shop.util.PixelmonItemHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category in the shop.
 */
public class ShopCategory {
    private final String id;
    private final String name;
    private final String iconId;
    private final boolean isPixelmonIcon;
    private final Material iconMaterial;
    private final List<String> description;
    private final int slot;
    private final List<String> items;

    /**
     * Create a new shop category.
     *
     * @param id            The unique identifier for this category
     * @param name          The display name
     * @param iconId        The material or Pixelmon item ID to use as the icon
     * @param isPixelmonIcon Whether the icon is a Pixelmon item
     * @param description   The description to show in the lore
     * @param slot          The slot in the main menu
     * @param items         List of item IDs in this category
     */
    public ShopCategory(String id, String name, String iconId, boolean isPixelmonIcon, List<String> description,
                        int slot, List<String> items) {
        this.id = id;
        this.name = name;
        this.iconId = iconId;
        this.isPixelmonIcon = isPixelmonIcon;

        // Set material for standard Minecraft items, or fallback material for Pixelmon items
        Material material = Material.STONE; // Default fallback
        try {
            if (!isPixelmonIcon) {
                material = Material.valueOf(iconId.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            Shop.getInstance().getLogger().warning("Invalid material for category " + id + ": " + iconId +
                    ". Using STONE as fallback.");
        }
        this.iconMaterial = material;

        this.description = description;
        this.slot = slot;
        this.items = items;
    }

    /**
     * Get the unique identifier for this category.
     *
     * @return The category ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the display name for this category.
     *
     * @return The category name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the material to use as the icon if not a Pixelmon item.
     *
     * @return The icon material
     */
    public Material getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Check if this category uses a Pixelmon item as its icon.
     *
     * @return True if the icon is a Pixelmon item, false otherwise
     */
    public boolean isPixelmonIcon() {
        return isPixelmonIcon;
    }

    /**
     * Get the ID of the icon (material name or Pixelmon item ID).
     *
     * @return The icon ID
     */
    public String getIconId() {
        return iconId;
    }

    /**
     * Get the description to show in the lore.
     *
     * @return The description
     */
    public List<String> getDescription() {
        return description;
    }

    /**
     * Get the slot in the main menu.
     *
     * @return The slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Get the list of item IDs in this category.
     *
     * @return The list of item IDs
     */
    public List<String> getItems() {
        return items;
    }


    /**
     * Create an item stack to represent this category in the GUI.
     *
     * @return The item stack
     */
    public ItemStack createIcon() {
        ItemStack item;

        // Create the appropriate item based on type
        if (isPixelmonIcon) {
            // Use the PixelmonItemHandler to create the item
            item = PixelmonItemHandler.createPixelmonItem(iconId, "§6" + name, null);

            // If creation failed, use the fallback material
            if (item == null) {
                Shop.getInstance().getLogger().warning("Failed to create Pixelmon icon for category " + id +
                        ", using fallback material");
                item = new ItemStack(iconMaterial);
            }
        } else {
            // Use standard Minecraft material
            item = new ItemStack(iconMaterial);
        }

        // Set the metadata
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + name);

            if (description != null && !description.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : description) {
                    lore.add("§7" + line);
                }
                lore.add("");
                lore.add("§e▶ Click to browse items");
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }}