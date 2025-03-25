package com.blissy.shop.models;

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
    private final Material icon;
    private final List<String> description;
    private final int slot;
    private final List<String> items;

    /**
     * Create a new shop category.
     *
     * @param id          The unique identifier for this category
     * @param name        The display name
     * @param icon        The material to use as the icon
     * @param description The description to show in the lore
     * @param slot        The slot in the main menu
     * @param items       List of item IDs in this category
     */
    public ShopCategory(String id, String name, Material icon, List<String> description, int slot, List<String> items) {
        this.id = id;
        this.name = name;
        this.icon = icon;
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
     * Get the material to use as the icon.
     *
     * @return The icon material
     */
    public Material getIcon() {
        return icon;
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
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6" + name);

            if (!description.isEmpty()) {
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
    }
}