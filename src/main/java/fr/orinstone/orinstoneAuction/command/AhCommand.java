package fr.orinstone.orinstoneAuction.command;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class AhCommand implements CommandExecutor, TabCompleter {
    private final OrinstoneAuctionPlugin plugin;

    public AhCommand(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            plugin.getAuctionMenu().openBrowse(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "my" -> plugin.getAuctionMenu().openMyListings(player, 0);
            case "expired" -> plugin.getAuctionMenu().openExpired(player, 0);
            case "history" -> plugin.getAuctionMenu().openHistory(player, 0);
            case "reload" -> {
                if (!player.hasPermission("orinstoneauction.admin")) {
                    plugin.getMessageUtil().send(player, "&cyou do not have permission");
                    return true;
                }
                plugin.reloadComponents();
                plugin.getMessageUtil().send(player, "&aconfig reloaded");
            }
            default -> plugin.getAuctionMenu().openBrowse(player, 0);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("my", "expired", "history", "reload");
        }
        return List.of();
    }
}
