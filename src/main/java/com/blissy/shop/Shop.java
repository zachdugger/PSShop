package com.blissy.shop;

import com.blissy.shop.commands.ShopCommand;
import com.blissy.shop.currency.CurrencyManager;
import com.blissy.shop.currency.GemCurrency;
import com.blissy.shop.currency.TokenCurrency;
import com.blissy.shop.currency.VaultCurrency;
import com.blissy.shop.gui.ShopGUI;
import com.blissy.shop.managers.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * Main class for the Shop plugin.
 */
public class Shop extends JavaPlugin {
    private static Shop instance;
    private CurrencyManager currencyManager;
    private ShopManager shopManager;
    private net.milkbowl.vault.economy.Economy vaultEconomy;
    private me.realized.tokenmanager.TokenManagerPlugin tokenManager;
    private com.blissy.gemextension.GemExtensionPlugin gemExtension;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config files
        saveDefaultConfig();
        saveResource("categories.yml", false);
        saveResource("items.yml", false);

        // Setup folder structure
        setupFolders();

        // Hook into dependencies
        if (!setupDependencies()) {
            getLogger().warning("Failed to hook into one or more required dependencies! Some features may not work.");
        }

        // Initialize currency manager
        currencyManager = new CurrencyManager();
        setupCurrencies();

        // Initialize shop manager
        shopManager = new ShopManager(this);
        shopManager.loadShopData();

        // Register shop command
        PluginCommand shopCommand = getCommand("shop");
        if (shopCommand == null) {
            getLogger().severe("Failed to get shop command! The plugin.yml might not be loaded correctly.");
        } else {
            ShopCommand shopCmd = new ShopCommand(this);
            shopCommand.setExecutor(shopCmd);
            shopCommand.setTabCompleter(shopCmd);
            getLogger().info("Shop command registered successfully!");
        }

        // Register TM/TR command
        registerTMTRCommand();

        getLogger().info("Shop has been enabled!");
    }
    @Override
    public void onDisable() {
        getLogger().info("Shop has been disabled!");
    }

    /**
     * Reload the plugin configuration and data
     * @return True if reload was successful, false otherwise
     */
    public boolean reload() {
        getLogger().info("Reloading Shop...");

        try {
            // Reload config
            reloadConfig();
            getLogger().info("Configuration reloaded");

            // Reinitialize shop manager
            shopManager.loadShopData();
            getLogger().info("Shop data reloaded");

            getLogger().info("Shop reload complete!");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading plugin", e);
            return false;
        }
    }

    private void setupFolders() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
    }

    private boolean setupDependencies() {
        boolean success = true;

        // Setup Vault Economy
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                    getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);

            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                getLogger().info("Successfully hooked into Vault economy!");
            } else {
                getLogger().warning("Failed to hook into Vault economy!");
                success = false;
            }
        } else {
            getLogger().warning("Vault not found! Server currency will not be available.");
            success = false;
        }

        // Setup TokenManager
        if (getServer().getPluginManager().getPlugin("TokenManager") != null) {
            try {
                tokenManager = (me.realized.tokenmanager.TokenManagerPlugin) getServer().getPluginManager().getPlugin("TokenManager");
                getLogger().info("Successfully hooked into TokenManager!");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into TokenManager!", e);
                success = false;
            }
        } else {
            getLogger().warning("TokenManager not found! Token currency will not be available.");
            success = false;
        }

        // Setup GemExtension
        if (getServer().getPluginManager().getPlugin("GemExtension") != null) {
            try {
                gemExtension = (com.blissy.gemextension.GemExtensionPlugin) getServer().getPluginManager().getPlugin("GemExtension");
                getLogger().info("Successfully hooked into GemExtension!");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into GemExtension!", e);
                success = false;
            }
        } else {
            getLogger().warning("GemExtension not found! Gem currency will not be available.");
            success = false;
        }

        return success;
    }

    private void setupCurrencies() {
        // Load enabled currencies from config
        if (vaultEconomy != null && getConfig().getBoolean("currencies.coins.enabled", true)) {
            currencyManager.registerCurrency(new VaultCurrency(vaultEconomy));
            getLogger().info("Registered Vault currency: " + vaultEconomy.currencyNamePlural());
        }

        if (tokenManager != null && getConfig().getBoolean("currencies.tokens.enabled", true)) {
            currencyManager.registerCurrency(new TokenCurrency(tokenManager));
            getLogger().info("Registered Token currency");
        }

        if (gemExtension != null && getConfig().getBoolean("currencies.gems.enabled", true)) {
            currencyManager.registerCurrency(new GemCurrency(gemExtension));
            getLogger().info("Registered Gem currency");
        }
    }

    /**
     * Get the instance of the plugin.
     * @return The plugin instance
     */
    public static Shop getInstance() {
        return instance;
    }

    /**
     * Get the currency manager.
     * @return The currency manager
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    /**
     * Get the shop manager.
     * @return The shop manager
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Get the Vault economy.
     * @return The Vault economy
     */
    public net.milkbowl.vault.economy.Economy getVaultEconomy() {
        return vaultEconomy;
    }

    /**
     * Get the TokenManager plugin.
     * @return The TokenManager plugin
     */
    public me.realized.tokenmanager.TokenManagerPlugin getTokenManager() {
        return tokenManager;
    }

    /**
     * Get the GemExtension plugin.
     * @return The GemExtension plugin
     */
    public com.blissy.gemextension.GemExtensionPlugin getGemExtension() {
        return gemExtension;
    }
    /**
     * Add all TMs and TRs to the shop
     * Call this method to populate your shop with all Pixelmon TMs and TRs
     * This will create type-based categories and add all available moves
     */
    public void addAllTMsAndTRs() {
        getLogger().info("Starting to add all TMs and TRs to the shop...");

        // Create and run the utility
        com.blissy.shop.util.AddTechnicalMovesToShop tmtrAdder =
                new com.blissy.shop.util.AddTechnicalMovesToShop(this);
        tmtrAdder.addAllTechnicalMoves();

        getLogger().info("Completed adding TMs and TRs to the shop!");
    }

    /**
     * Create a command to add all TMs and TRs to the shop
     * This can be called by admins in-game
     */
    public void registerTMTRCommand() {
        PluginCommand command = getCommand("shopaddtmtr");
        if (command != null) {
            command.setExecutor(new CommandExecutor() {
                @Override
                public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
                    if (!sender.hasPermission("shop.admin")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                        return true;
                    }

                    sender.sendMessage(ChatColor.GOLD + "Starting to add all TMs and TRs to the shop...");
                    sender.sendMessage(ChatColor.GOLD + "This may take a moment, please be patient.");

                    // Run in async task to prevent server lag
                    Bukkit.getScheduler().runTaskAsynchronously(Shop.this, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                addAllTMsAndTRs();

                                // Switch back to main thread to notify player
                                Bukkit.getScheduler().runTask(Shop.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        sender.sendMessage(ChatColor.GREEN + "Successfully added all TMs and TRs to the shop!");
                                        sender.sendMessage(ChatColor.GREEN + "Use /shop reload to apply changes.");
                                    }
                                });
                            } catch (Exception e) {
                                getLogger().log(Level.SEVERE, "Error adding TMs and TRs", e);

                                // Switch back to main thread to notify player
                                Bukkit.getScheduler().runTask(Shop.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        sender.sendMessage(ChatColor.RED + "Error adding TMs and TRs. Check console for details.");
                                    }
                                });
                            }
                        }
                    });

                    return true;
                }
            });
            getLogger().info("Registered shopaddtmtr command!");
        } else {
            getLogger().warning("Failed to register shopaddtmtr command! Add it to plugin.yml first.");
        }
    }
}