package fr.orinstone.orinstoneAuction.command;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AhSellCommand implements CommandExecutor, TabCompleter {
    private final OrinstoneAuctionPlugin plugin;

    public AhSellCommand(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            plugin.getMessageUtil().send(player, "&eusage: /ahsell <price>");
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
            if (price <= 0.0D) {
                throw new NumberFormatException("price must be positive");
            }
        } catch (NumberFormatException exception) {
            plugin.getMessageUtil().send(player, "&cinvalid price");
            return true;
        }

        if (!plugin.getDiscordLinkService().canSell(player)) {
            plugin.getMessageUtil().send(player, plugin.getConfig().getString("messages.discord-link-required", "&cyou must link your discord account before selling. use &f/discord link"));
            return true;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType() == Material.AIR) {
            plugin.getMessageUtil().send(player, "&cyou need to hold an item in your main hand");
            return true;
        }

        ItemStack toSell = inHand.clone();
        try {
            plugin.getAuctionService().createListing(player, toSell, price);
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            plugin.getMessageUtil().send(player, "&ayour item is now listed for &f" + plugin.getEconomyService().format(price));
        } catch (IllegalStateException exception) {
            plugin.getMessageUtil().send(player, "&c" + exception.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
