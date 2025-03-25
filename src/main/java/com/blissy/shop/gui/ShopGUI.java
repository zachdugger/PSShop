package com.blissy.shop.gui;

import com.blissy.shop.Shop;
import com.blissy.shop.models.ShopCategory;
import com.blissy.shop.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Handles the shop GUI.
 */
public class ShopGUI implements Listener {
    private final Shop plugin;
    private final Map<UUID, GUISession> playerSessions = new HashMap<>();

    // Constants for inventory sizes
    private static final int MAIN_MENU_SIZE = 54; // 6 rows
    private static final int CATEGORY_MENU_SIZE = 54; // 6 rows

    // Title prefixes
    private static final String MAIN_MENU_TITLE = ChatColor.GOLD + "Shop - Main Menu";
    private static final String CATEGORY_MENU_PREFIX = ChatColor.GOLD + "Shop - ";

    /**
     * Create a new shop GUI.
     *
     * @param plugin The plugin instance
     */
    public ShopGUI(Shop plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the main menu for a player.
     *
     * @param player The player
     */
    public void openMainMenu(Player player) {
        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, MAIN_MENU_SIZE, MAIN_MENU_TITLE);

        // Get categories ordered by slot
        List<ShopCategory> orderedCategories = new ArrayList<>(plugin.getShopManager().getCategories().values());
        orderedCategories.sort(Comparator.comparingInt(ShopCategory::getSlot));

        // Add category items
        for (ShopCategory category : orderedCategories) {
            int slot = category.getSlot();

            // Ensure slot is within bounds
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, category.createIcon());
            }
        }

        // Add navigation/utility items
        addBorder(inventory);

        // Open inventory
        player.openInventory(inventory);

        // Store session
        playerSessions.put(player.getUniqueId(), new GUISession(GUIType.MAIN_MENU, null, null));
    }

    /**
     * Open a category menu for a player.
     *
     * @param player The player
     * @param categoryId The category ID
     */
    public void openCategoryMenu(Player player, String categoryId) {
        ShopCategory category = plugin.getShopManager().getCategory(categoryId);

        if (category == null) {
            player.sendMessage(ChatColor.RED + "Category not found!");
            return;
        }

        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, CATEGORY_MENU_SIZE,
                CATEGORY_MENU_PREFIX + category.getName());

        // Get items
        List<ShopItem> items = plugin.getShopManager().getItemsInCategory(categoryId);

        // Sort items by slot
        items.sort(Comparator.comparingInt(ShopItem::getSlot));

        // Add items
        for (ShopItem item : items) {
            int slot = item.getSlot();

            // Ensure slot is within bounds
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.createIcon(true));
            }
        }

        // Add navigation items
        addBorder(inventory);

        // Add back button
        ItemStack backButton = createItem(Material.ARROW, ChatColor.RED + "Back to Main Menu",
                ChatColor.GRAY + "Click to return to the main menu");
        inventory.setItem(49, backButton);

        // Open inventory
        player.openInventory(inventory);

        // Store session
        playerSessions.put(player.getUniqueId(), new GUISession(GUIType.CATEGORY_MENU, categoryId, null));
    }

    /**
     * Handle inventory click events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GUISession session = playerSessions.get(player.getUniqueId());

        if (session == null || event.getClickedInventory() == null) {
            return;
        }

        // Cancel the event to prevent item moving
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Handle based on GUI type
        if (session.getType() == GUIType.MAIN_MENU) {
            handleMainMenuClick(player, event.getSlot());
        } else if (session.getType() == GUIType.CATEGORY_MENU) {
            handleCategoryMenuClick(player, event.getSlot(), event.isShiftClick(), session.getCategoryId());
        }
    }

    /**
     * Handle clicks in the main menu.
     *
     * @param player The player
     * @param slot The clicked slot
     */
    private void handleMainMenuClick(Player player, int slot) {
        // Check each category's slot
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            if (category.getSlot() == slot) {
                openCategoryMenu(player, category.getId());
                return;
            }
        }
    }

    /**
     * Handle clicks in a category menu.
     *
     * @param player The player
     * @param slot The clicked slot
     * @param isShiftClick Whether the click was a shift-click
     * @param categoryId The category ID
     */
    private void handleCategoryMenuClick(Player player, int slot, boolean isShiftClick, String categoryId) {
        // Check if back button was clicked (slot 49)
        if (slot == 49) {
            openMainMenu(player);
            return;
        }

        // Check each item's slot
        for (ShopItem item : plugin.getShopManager().getItemsInCategory(categoryId)) {
            if (item.getSlot() == slot) {
                int quantity = isShiftClick ? item.getAmount() * 64 : item.getAmount();
                plugin.getShopManager().purchaseItem(player, item, quantity);
                return;
            }
        }
    }

    /**
     * Handle inventory close events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            playerSessions.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Add a border to an inventory.
     *
     * @param inventory The inventory
     */
    private void addBorder(Inventory inventory) {
        int size = inventory.getSize();

        // Material for the border
        Material borderMaterial = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack borderItem = createItem(borderMaterial, " ", "");

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem); // Top row
            inventory.setItem(size - 9 + i, borderItem); // Bottom row
        }

        // Left and right columns (excluding corners)
        for (int i = 1; i < size / 9 - 1; i++) {
            inventory.setItem(i * 9, borderItem); // Left column
            inventory.setItem(i * 9 + 8, borderItem); // Right column
        }
    }

    /**
     * Create an item stack with the specified metadata.
     *
     * @param material The material
     * @param name The display name
     * @param lore The lore lines
     * @return The item stack
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Enum for GUI types.
     */
    private enum GUIType {
        MAIN_MENU,
        CATEGORY_MENU
    }

    /**
     * Class to track a player's GUI session.
     */
    private static class GUISession {
        private final GUIType type;
        private final String categoryId;
        private final String itemId;

        /**
         * Create a new GUI session.
         *
         * @param type The GUI type
         * @param categoryId The category ID (may be null)
         * @param itemId The item ID (may be null)
         */
        public GUISession(GUIType type, String categoryId, String itemId) {
            this.type = type;
            this.categoryId = categoryId;
            this.itemId = itemId;
        }

        /**
         * Get the GUI type.
         *
         * @return The GUI type
         */
        public GUIType getType() {
            return type;
        }

        /**
         * Get the category ID.
         *
         * @return The category ID
         */
        public String getCategoryId() {
            return categoryId;
        }

        /**
         * Get the item ID.
         *
         * @return The item ID
         */
        public String getItemId() {
            return itemId;
        }
    }
}