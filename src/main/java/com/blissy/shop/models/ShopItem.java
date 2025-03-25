package com.blissy.shop.models;

import com.blissy.shop.Shop;
import com.blissy.shop.currency.Currency;
import com.blissy.shop.util.PixelmonItemHandler;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

/**
 * Represents an item that can be purchased in the shop.
 * Uses reflection-based handlers for Pixelmon items.
 */
public class ShopItem {
    private final String id;
    private final String name;
    private final Material material;
    private final int amount;
    private final List<String> description;
    private final String currencyId;
    private final long price;
    private final int slot;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean glowing;

    // For Pixelmon specific items
    private final String pixelmonItemId;
    private final short pixelmonDamage;
    private final Map<String, Object> pixelmonNbt;

    /**
     * Create a new shop item.
     */
    public ShopItem(String id, String name, Material material, int amount, List<String> description,
                    String currencyId, long price, int slot, Map<Enchantment, Integer> enchantments,
                    boolean glowing, String pixelmonItemId, short pixelmonDamage, Map<String, Object> pixelmonNbt) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.amount = amount;
        this.description = description;
        this.currencyId = currencyId;
        this.price = price;
        this.slot = slot;
        this.enchantments = enchantments;
        this.glowing = glowing;
        this.pixelmonItemId = pixelmonItemId;
        this.pixelmonDamage = pixelmonDamage;
        this.pixelmonNbt = pixelmonNbt;
    }

    // Standard getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public List<String> getDescription() { return description; }
    public String getCurrencyId() { return currencyId; }
    public long getPrice() { return price; }
    public int getSlot() { return slot; }
    public String getPixelmonItemId() { return pixelmonItemId; }
    public short getPixelmonDamage() { return pixelmonDamage; }
    public Map<String, Object> getPixelmonNbt() { return pixelmonNbt; }

    /**
     * Get the currency used for this item.
     */
    public Optional<Currency> getCurrency() {
        return Shop.getInstance().getCurrencyManager().getCurrency(currencyId);
    }

    /**
     * Create an item stack to represent this item in the GUI.
     */
    public ItemStack createIcon(boolean showBuyInfo) {
        ItemStack item;

        // Check if this is a Pixelmon item
        if (pixelmonItemId != null && !pixelmonItemId.isEmpty()) {
            // Use our handler to create the Pixelmon item
            item = PixelmonItemHandler.createPixelmonItem(pixelmonItemId, "§6" + name, null);
        } else {
            item = new ItemStack(material, 1);
        }

        // Customize the item meta
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + name);

            List<String> lore = new ArrayList<>();

            // Add description
            if (description != null && !description.isEmpty()) {
                for (String line : description) {
                    lore.add("§7" + line);
                }
                lore.add("");
            }

            // Add buy information if requested
            if (showBuyInfo) {
                Optional<Currency> currency = getCurrency();

                if (currency.isPresent()) {
                    lore.add("§ePrice: " + currency.get().getColoredName() + " §f" + currency.get().formatAmount(price));
                    lore.add("");
                    lore.add("§aLeft-click to buy one");
                    lore.add("§aShift-click to buy a stack");
                }
            }

            meta.setLore(lore);

            // Add enchantments
            if (enchantments != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            // Add glowing effect without showing enchantments
            if (glowing) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a purchasable item stack.
     */
    public ItemStack createPurchasableItem() {
        ItemStack item;

        // Create the Pixelmon item if specified
        if (pixelmonItemId != null && !pixelmonItemId.isEmpty()) {
            // Use our handler to create the Pixelmon item
            item = PixelmonItemHandler.createPixelmonItem(pixelmonItemId, "§r" + name, description);

            // If creation failed, use the fallback material
            if (item == null) {
                Shop.getInstance().getLogger().warning("Failed to create Pixelmon item " + pixelmonItemId + ", using fallback material");
                item = new ItemStack(material, amount);
            }
        } else {
            item = new ItemStack(material, amount);
        }

        // Set the amount
        item.setAmount(amount);

        // Customize the item if needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName("§r" + name);
            }

            // Add description as lore if it exists
            if (description != null && !description.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : description) {
                    if (!line.contains("Price:") && !line.contains("Click to")) {
                        lore.add("§7" + line);
                    }
                }

                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }
            }

            // Add enchantments
            if (enchantments != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a builder for this class to make creating shop items easier.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder class for creating ShopItem instances.
     */
    public static class Builder {
        private final String id;
        private String name = "";
        private Material material = Material.STONE;
        private int amount = 1;
        private List<String> description = new ArrayList<>();
        private String currencyId = "coins";
        private long price = 0;
        private int slot = 0;
        private Map<Enchantment, Integer> enchantments = new HashMap<>();
        private boolean glowing = false;
        private String pixelmonItemId = null;
        private short pixelmonDamage = 0;
        private Map<String, Object> pixelmonNbt = new HashMap<>();

        // Builder methods
        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(List<String> description) {
            this.description = description;
            return this;
        }

        public Builder currencyId(String currencyId) {
            this.currencyId = currencyId;
            return this;
        }

        public Builder price(long price) {
            this.price = price;
            return this;
        }

        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        public Builder enchantment(Enchantment enchantment, int level) {
            this.enchantments.put(enchantment, level);
            return this;
        }

        public Builder glowing(boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        public Builder pixelmonItemId(String pixelmonItemId) {
            this.pixelmonItemId = pixelmonItemId;
            return this;
        }

        public Builder pixelmonDamage(short pixelmonDamage) {
            this.pixelmonDamage = pixelmonDamage;
            return this;
        }

        public Builder pixelmonNbt(String key, Object value) {
            this.pixelmonNbt.put(key, value);
            return this;
        }

        public ShopItem build() {
            return new ShopItem(id, name, material, amount, description, currencyId, price, slot,
                    enchantments, glowing, pixelmonItemId, pixelmonDamage, pixelmonNbt);
        }
    }
}