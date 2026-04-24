package fr.orinstone.orinstoneAuction.service;

import github.scarsz.discordsrv.DiscordSRV;
import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordLinkService {
    private final OrinstoneAuctionPlugin plugin;
    private final boolean requireLinkedToSell;
    private final boolean allowIfDiscordSrvMissing;

    public DiscordLinkService(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
        this.requireLinkedToSell = plugin.getConfig().getBoolean("discordsrv.require-linked-to-sell", true);
        this.allowIfDiscordSrvMissing = plugin.getConfig().getBoolean("discordsrv.allow-selling-if-missing", false);
    }

    public boolean isRequiredToSell() {
        return requireLinkedToSell;
    }

    public boolean isDiscordSrvAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    public String getDiscordId(Player player) {
        if (!isDiscordSrvAvailable()) return null;
        try {
            return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not read DiscordSRV link for " + player.getName() + ": " + throwable.getMessage());
            return null;
        }
    }

    public boolean canSell(Player player) {
        if (!requireLinkedToSell) return true;
        if (!isDiscordSrvAvailable()) return allowIfDiscordSrvMissing || player.hasPermission("orinstoneauction.admin");
        return getDiscordId(player) != null || player.hasPermission("orinstoneauction.admin");
    }
}
