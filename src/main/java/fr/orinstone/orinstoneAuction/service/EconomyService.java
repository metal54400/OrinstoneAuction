package fr.orinstone.orinstoneAuction.service;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyService {
    private final OrinstoneAuctionPlugin plugin;
    private Economy economy;

    public EconomyService(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        this.economy = provider.getProvider();
        return this.economy != null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.format("%.2f", amount);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("failed economy withdraw: " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("failed economy deposit: " + response.errorMessage);
        }
        return response.transactionSuccess();
    }
}
