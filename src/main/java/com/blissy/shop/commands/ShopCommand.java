package com.blissy.shop.commands;

import com.blissy.shop.Shop;
import com.blissy.shop.gui.ShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Handler for the shop command.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {
    private final Shop plugin;
    private final ShopGUI shopGUI;

    /**
     * Create a new shop command handler.
     *
     * @param plugin The plugin instance
     */
    public ShopCommand(Shop plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        plugin.getLogger().info("ShopCommand handler initialized");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("ShopCommand executed by " + sender.getName() + " with args: " + Arrays.toString(args));

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("shop.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            plugin.getLogger().info("Player " + player.getName() + " lacks shop.use permission");
            return true;
        }

        if (args.length == 0) {
            // Open the main shop menu
            plugin.getLogger().info("Opening shop main menu for " + player.getName());
            try {
                shopGUI.openMainMenu(player);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error opening shop GUI", e);
                player.sendMessage(ChatColor.RED + "An error occurred while opening the shop GUI.");
            }
            return true;
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        plugin.getLogger().info("Processing subcommand: " + subCommand);

        switch (subCommand) {
            case "category":
            case "cat":
                // Open a specific category
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /shop category <categoryId>");
                    return true;
                }

                String categoryId = args[1].toLowerCase();
                if (plugin.getShopManager().getCategory(categoryId) == null) {
                    player.sendMessage(ChatColor.RED + "Category not found: " + categoryId);
                    return true;
                }

                shopGUI.openCategoryMenu(player, categoryId);
                break;

            case "help":
                // Show help
                showHelp(player);
                break;

            case "reload":
                // Reload the plugin (admin only)
                if (!player.hasPermission("shop.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
                    return true;
                }

                player.sendMessage(ChatColor.GOLD + "Reloading Shop plugin...");
                boolean success = plugin.reload();

                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Shop has been successfully reloaded!");
                } else {
                    player.sendMessage(ChatColor.RED + "Error reloading Shop. Check console for details.");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                showHelp(player);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("category", "help"));

            // Only add admin commands for players with permission
            if (sender.hasPermission("shop.admin")) {
                completions.add("reload");
            }

            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("category") || args[0].equalsIgnoreCase("cat"))) {
            return plugin.getShopManager().getCategories().keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Show the help message.
     *
     * @param player The player
     */
    private void showHelp(Player player) {
        plugin.getLogger().info("Showing help to " + player.getName());
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== Shop Commands =====");
        player.sendMessage(ChatColor.GOLD + "/shop" + ChatColor.WHITE + " - Open the main shop menu");
        player.sendMessage(ChatColor.GOLD + "/shop category <categoryId>" + ChatColor.WHITE + " - Open a specific category");
        player.sendMessage(ChatColor.GOLD + "/shop help" + ChatColor.WHITE + " - Show this help message");

        // Only show admin commands to users with the right permission
        if (player.hasPermission("shop.admin")) {
            player.sendMessage(ChatColor.GOLD + "/shop reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        }

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===========================");
    }
}