package com.blissy.shop.models;

import com.blissy.shop.Shop;
import com.blissy.shop.currency.Currency;
import com.pixelmonmod.api.registry.RegistryManager;
import com.pixelmonmod.api.registry.RegistryValue;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import net.minecraft.item.Item;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Represents an item that can be purchased in the shop.
 * This version directly uses Pixelmon's Registry API for item creation.
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
            item = createPixelmonItemStack();
        } else {
            item = new ItemStack(material, 1);
        }

        // If item creation failed, log error and return null
        if (item == null) {
            Shop.getInstance().getLogger().severe("Failed to create item: " + pixelmonItemId);
            return null;
        }

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
            item = createPixelmonItemStack();
        } else {
            item = new ItemStack(material, amount);
        }

        // If item creation failed, log error and return null
        if (item == null) {
            Shop.getInstance().getLogger().severe("Failed to create item: " + pixelmonItemId);
            return null;
        }

        // Set the amount
        item.setAmount(amount);

        // Customize the item if needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!name.equals("")) {
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
     * Create a Pixelmon ItemStack using direct registry access.
     * This method uses Pixelmon's API to create the item.
     */
    private ItemStack createPixelmonItemStack() {
        try {
            // Normalize the item ID
            String normalizedId = pixelmonItemId;
            if (!normalizedId.contains(":")) {
                normalizedId = "pixelmon:" + normalizedId;
            }

            // Special handling for Pokeballs using PokeBallRegistry
            if (isPokeBall(normalizedId)) {
                return createPokeballItem(normalizedId);
            }

            // Special handling for TMs
            if (normalizedId.contains("tm_") || normalizedId.startsWith("pixelmon:tm")) {
                return createTMItem(normalizedId);
            }

            // Get the item from Pixelmon's registry
            Optional<RegistryValue<Item>> itemRegistryValue = RegistryManager.get(Item.class, normalizedId);

            if (itemRegistryValue.isPresent() && itemRegistryValue.get().isInitialized()) {
                // Get the Minecraft item from the registry
                Item nmsItem = itemRegistryValue.get().getValueUnsafe();

                // Create the NMS ItemStack
                net.minecraft.item.ItemStack nmsStack = new net.minecraft.item.ItemStack(nmsItem);

                // Set damage value if needed
                if (pixelmonDamage > 0) {
                    nmsStack.setDamage(pixelmonDamage);
                }

                // Apply any NBT data
                if (!pixelmonNbt.isEmpty()) {
                    net.minecraft.nbt.CompoundNBT nbt = nmsStack.getOrCreateTag();

                    for (Map.Entry<String, Object> entry : pixelmonNbt.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        applyNbtValue(nbt, key, value);
                    }
                }

                // Convert to Bukkit ItemStack
                return CraftItemStack.asBukkitCopy(nmsStack);
            } else {
                Shop.getInstance().getLogger().warning("Item not found in Pixelmon registry: " + normalizedId);
                return null;
            }
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.SEVERE, "Error creating Pixelmon item: " + pixelmonItemId, e);
            return null;
        }
    }

    /**
     * Check if the item is a Pokeball using IDs from PokeBallRegistry
     */
    private boolean isPokeBall(String itemId) {
        // Extract base name without prefix or suffix
        String baseName = itemId;
        if (baseName.contains(":")) {
            baseName = baseName.split(":")[1];
        }

        // Check if it's in the default Pokeball IDs list
        for (String pokeballId : PokeBallRegistry.DEFAULT_POKE_BALL_IDS) {
            if (pokeballId.equals(baseName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Apply NBT value to a CompoundNBT tag.
     */
    private void applyNbtValue(net.minecraft.nbt.CompoundNBT nbt, String key, Object value) {
        if (value instanceof String) {
            nbt.putString(key, (String) value);
        } else if (value instanceof Integer) {
            nbt.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            nbt.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            nbt.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            nbt.putDouble(key, (Double) value);
        } else if (value instanceof Long) {
            nbt.putLong(key, (Long) value);
        } else if (value instanceof Byte) {
            nbt.putByte(key, (Byte) value);
        } else if (value instanceof Short) {
            nbt.putShort(key, (Short) value);
        } else {
            // Default to string representation
            nbt.putString(key, value.toString());
        }
    }

    /**
     * Create a TM item using Pixelmon's API.
     */
    private ItemStack createTMItem(String itemId) {
        try {
            // Extract move name
            String moveName = null;

            // Try to get move from NBT data
            if (pixelmonNbt.containsKey("move")) {
                moveName = pixelmonNbt.get("move").toString();
            } else {
                // Try to extract from ID
                String[] parts = itemId.split("_");
                if (parts.length > 1) {
                    moveName = parts[1];
                }
            }

            if (moveName != null) {
                // Use reflection to access Pixelmon's TM creation API
                Class<?> tmClass = Class.forName("com.pixelmonmod.pixelmon.api.items.PixelmonItemsTMs");
                Method createTMMethod = tmClass.getMethod("createTMStack", String.class);
                net.minecraft.item.ItemStack nmsStack = (net.minecraft.item.ItemStack) createTMMethod.invoke(null, moveName);

                if (nmsStack != null) {
                    return CraftItemStack.asBukkitCopy(nmsStack);
                }
            }

            return null;
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING, "Error creating TM item: " + itemId, e);
            return null;
        }
    }

    /**
     * Create a Pokeball item using PokeBallRegistry.
     */
    private ItemStack createPokeballItem(String itemId) {
        try {
            // Extract ball type from ID
            String ballType = itemId;
            if (ballType.contains(":")) {
                ballType = ballType.split(":")[1];
            }

            // Use PokeBallRegistry to get the PokeBall
            RegistryValue<PokeBall> pokeBallValue = PokeBallRegistry.getPokeBall(ballType);
            if (pokeBallValue.isInitialized()) {
                // Get the Pokeball from registry
                PokeBall pokeBall = pokeBallValue.getValueUnsafe();

                // Access the Minecraft item for this PokeBall
                Class<?> pokeBallClass = pokeBall.getClass();
                Method getItemMethod = pokeBallClass.getMethod("getItem");
                Item nmsItem = (Item) getItemMethod.invoke(pokeBall);

                if (nmsItem != null) {
                    net.minecraft.item.ItemStack nmsStack = new net.minecraft.item.ItemStack(nmsItem);
                    return CraftItemStack.asBukkitCopy(nmsStack);
                }
            }

            return null;
        } catch (Exception e) {
            Shop.getInstance().getLogger().log(Level.WARNING, "Error creating Pokeball item: " + itemId, e);
            return null;
        }
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