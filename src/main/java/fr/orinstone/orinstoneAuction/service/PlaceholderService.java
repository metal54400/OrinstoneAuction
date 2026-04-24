package fr.orinstone.orinstoneAuction.service;

import me.clip.placeholderapi.PlaceholderAPI;
import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import org.bukkit.entity.Player;

public class PlaceholderService {
    private final OrinstoneAuctionPlugin plugin;
    private final boolean enabled;

    public PlaceholderService(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("placeholderapi.enabled", true)
            && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String apply(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (!enabled || player == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
