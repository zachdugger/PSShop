package com.blissy.shop.managers;

import com.blissy.shop.Shop;
import com.blissy.shop.models.ShopCategory;
import com.blissy.shop.models.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages shop data and operations.
 */
public class ShopManager {
    private final Shop plugin;
    private final Map<String, ShopCategory> categories = new HashMap<>();
    private final Map<String, ShopItem> items = new HashMap<>();

    /**
     * Create a new shop manager.
     *
     * @param plugin The plugin instance
     */
    public ShopManager(Shop plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all shop data from configuration files.
     */
    public void loadShopData() {
        // Clear existing data
        categories.clear();
        items.clear();

        // Load categories
        loadCategories();

        // Load items
        loadItems();

        plugin.getLogger().info("Loaded " + categories.size() + " categories and " + items.size() + " items.");
    }

    /**
     * Load categories from the categories.yml file.
     */
    private void loadCategories() {
        File categoriesFile = new File(plugin.getDataFolder(), "categories.yml");

        if (!categoriesFile.exists()) {
            plugin.getLogger().warning("categories.yml file not found! Creating a default one.");
            plugin.saveResource("categories.yml", true);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(categoriesFile);
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");

        if (categoriesSection == null) {
            plugin.getLogger().warning("No categories found in categories.yml!");
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            try {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);

                if (categorySection == null) {
                    continue;
                }

                String name = categorySection.getString("name", categoryId);
                String iconId = categorySection.getString("icon", "STONE");
                boolean isPixelmonIcon = categorySection.getBoolean("pixelmon_icon", false);
                List<String> description = categorySection.getStringList("description");
                int slot = categorySection.getInt("slot", 0);
                List<String> itemList = categorySection.getStringList("items");

                ShopCategory category = new ShopCategory(categoryId, name, iconId, isPixelmonIcon, description, slot, itemList);
                categories.put(categoryId, category);

                plugin.getLogger().info("Loaded category: " + categoryId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading category " + categoryId, e);
            }
        }
    }

    /**
     * Load items from the items.yml file.
     */
    private void loadItems() {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            plugin.getLogger().warning("items.yml file not found! Creating a default one.");
            plugin.saveResource("items.yml", true);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");

        if (itemsSection == null) {
            plugin.getLogger().warning("No items found in items.yml!");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            try {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);

                if (itemSection == null) {
                    continue;
                }

                // Start building the item
                ShopItem.Builder builder = ShopItem.builder(itemId);

                // Basic properties
                builder.name(ChatColor.translateAlternateColorCodes('&', itemSection.getString("name", "")));

                String materialStr = itemSection.getString("material", "STONE");
                Material material;
                try {
                    material = Material.valueOf(materialStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for item " + itemId + ": " + materialStr);
                    material = Material.STONE;
                }
                builder.material(material);

                builder.amount(itemSection.getInt("amount", 1));

                // Description/lore
                List<String> rawDescription = itemSection.getStringList("description");
                List<String> description = rawDescription.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                builder.description(description);

                // Pricing
                builder.currencyId(itemSection.getString("currency", "coins"));
                builder.price(itemSection.getLong("price", 0));

                // GUI placement
                builder.slot(itemSection.getInt("slot", 0));

                // Visual effects
                builder.glowing(itemSection.getBoolean("glowing", false));

                // Enchantments
                ConfigurationSection enchantmentsSection = itemSection.getConfigurationSection("enchantments");
                if (enchantmentsSection != null) {
                    for (String enchantmentName : enchantmentsSection.getKeys(false)) {
                        try {
                            // Fix for deprecated getByName method - use Enchantment.REGISTRY
                            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName.toLowerCase()));
                            if (enchantment != null) {
                                int level = enchantmentsSection.getInt(enchantmentName, 1);
                                builder.enchantment(enchantment, level);
                            } else {
                                plugin.getLogger().warning("Enchantment not found: " + enchantmentName);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Invalid enchantment: " + enchantmentName);
                        }
                    }
                }

                // Pixelmon specific properties
                builder.pixelmonItemId(itemSection.getString("pixelmon_id", null));
                builder.pixelmonDamage((short) itemSection.getInt("pixelmon_damage", 0));

                // Pixelmon NBT data
                ConfigurationSection nbtSection = itemSection.getConfigurationSection("pixelmon_nbt");
                if (nbtSection != null) {
                    for (String key : nbtSection.getKeys(false)) {
                        builder.pixelmonNbt(key, nbtSection.get(key));
                    }
                }

                // Build and store the item
                ShopItem item = builder.build();
                items.put(itemId, item);

                plugin.getLogger().info("Loaded item: " + itemId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading item " + itemId, e);
            }
        }
    }

    /**
     * Get all categories.
     *
     * @return Map of category ID to category
     */
    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    /**
     * Get a category by ID.
     *
     * @param id The category ID
     * @return The category, or null if not found
     */
    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }

    /**
     * Get all items.
     *
     * @return Map of item ID to item
     */
    public Map<String, ShopItem> getItems() {
        return items;
    }

    /**
     * Get an item by ID.
     *
     * @param id The item ID
     * @return The item, or null if not found
     */
    public ShopItem getItem(String id) {
        return items.get(id);
    }

    /**
     * Get all items in a category.
     *
     * @param categoryId The category ID
     * @return List of items in the category
     */
    public List<ShopItem> getItemsInCategory(String categoryId) {
        ShopCategory category = getCategory(categoryId);

        if (category == null) {
            return new ArrayList<>();
        }

        return category.getItems().stream()
                .map(this::getItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Purchase an item for a player.
     *
     * @param player The player
     * @param item The item to purchase
     * @param quantity The quantity to purchase
     * @return True if the purchase was successful, false otherwise
     */
    public boolean purchaseItem(Player player, ShopItem item, int quantity) {
        // Check if the player has enough currency
        Optional<com.blissy.shop.currency.Currency> optionalCurrency = item.getCurrency();

        if (!optionalCurrency.isPresent()) {
            player.sendMessage(ChatColor.RED + "Invalid currency for this item!");
            return false;
        }

        com.blissy.shop.currency.Currency currency = optionalCurrency.get();
        long totalPrice = item.getPrice() * quantity;

        if (!currency.hasBalance(player, totalPrice)) {
            player.sendMessage(ChatColor.RED + "You don't have enough " + currency.getName() + "!");
            player.sendMessage(ChatColor.RED + "Required: " + currency.formatAmount(totalPrice) +
                    ", You have: " + currency.formatAmount(currency.getBalance(player)));
            return false;
        }

        // Create the item stack
        ItemStack itemStack = item.createPurchasableItem();
        itemStack.setAmount(quantity);

        // Check if the player has enough inventory space
        if (!hasSpace(player, itemStack)) {
            player.sendMessage(ChatColor.RED + "You don't have enough inventory space!");
            return false;
        }

        // Withdraw the currency
        if (!currency.withdraw(player, totalPrice)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw " + currency.formatAmount(totalPrice) + "!");
            return false;
        }

        // Give the item to the player
        player.getInventory().addItem(itemStack);

        // Send success message
        player.sendMessage(ChatColor.GREEN + "You purchased " + ChatColor.YELLOW + quantity + "x " +
                item.getName() + ChatColor.GREEN + " for " + currency.getColoredName() +
                " " + currency.formatAmount(totalPrice) + ChatColor.GREEN + "!");

        return true;
    }

    /**
     * Check if a player has enough inventory space for an item.
     *
     * @param player The player
     * @param item The item
     * @return True if the player has enough space, false otherwise
     */
    private boolean hasSpace(Player player, ItemStack item) {
        // Calculate how many of this item can fit in a single slot
        int maxStackSize = item.getMaxStackSize();
        int amount = item.getAmount();

        // Calculate how many slots are needed
        int slotsNeeded = (int) Math.ceil((double) amount / maxStackSize);

        // Count empty slots in the player's inventory
        int emptySlots = 0;
        for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                emptySlots++;

                // If we have enough empty slots, return early
                if (emptySlots >= slotsNeeded) {
                    return true;
                }
            }
        }

        // If we don't have enough empty slots, we need to check for similar items that can stack
        // Fix: Use getStorageContents() to get a defensive copy of the inventory contents
        ItemStack[] contents = player.getInventory().getStorageContents();
        Map<Integer, ItemStack> similarItems = new HashMap<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack currentItem = contents[i];
            if (currentItem != null && currentItem.getType() == item.getType()) {
                similarItems.put(i, currentItem);
            }
        }

        for (ItemStack similarItem : similarItems.values()) {
            // Skip if the items don't stack (e.g., different metadata)
            if (!similarItem.isSimilar(item)) {
                continue;
            }

            // Calculate how much space is left in this stack
            int spaceInStack = maxStackSize - similarItem.getAmount();

            if (spaceInStack > 0) {
                // Subtract the space in this stack from the amount needed
                amount -= spaceInStack;

                // If we've accounted for all items, return true
                if (amount <= 0) {
                    return true;
                }

                // Recalculate slots needed
                slotsNeeded = (int) Math.ceil((double) amount / maxStackSize);

                // If the empty slots we have can hold the rest, return true
                if (emptySlots >= slotsNeeded) {
                    return true;
                }
            }
        }

        // If we get here, the player doesn't have enough space
        return false;
    }
}