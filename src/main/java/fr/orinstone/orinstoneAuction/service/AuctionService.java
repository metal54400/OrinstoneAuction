package fr.orinstone.orinstoneAuction.service;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import fr.orinstone.orinstoneAuction.model.AuctionItem;
import fr.orinstone.orinstoneAuction.model.ExpiredClaim;
import fr.orinstone.orinstoneAuction.model.SaleHistory;
import fr.orinstone.orinstoneAuction.storage.AuctionRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionService {
    private final OrinstoneAuctionPlugin plugin;
    private final AuctionRepository repository;
    private final EconomyService economyService;
    private final int listingDurationHours;
    private final double listingFee;
    private final double saleTaxPercent;
    private final int maxListingsPerPlayer;

    public AuctionService(OrinstoneAuctionPlugin plugin, AuctionRepository repository, EconomyService economyService) {
        this.plugin = plugin;
        this.repository = repository;
        this.economyService = economyService;
        this.listingDurationHours = plugin.getConfig().getInt("auction.listing-duration-hours", 48);
        this.listingFee = plugin.getConfig().getDouble("auction.listing-fee", 0.0D);
        this.saleTaxPercent = plugin.getConfig().getDouble("auction.sale-tax-percent", 5.0D);
        this.maxListingsPerPlayer = plugin.getConfig().getInt("auction.max-listings-per-player", 10);
    }

    public long createListing(Player seller, ItemStack itemStack, double price) {
        double minimum = getMinimumPrice(itemStack.getType());
        if (minimum > 0.0D && price < minimum) {
            throw new IllegalStateException("minimum price for " + itemStack.getType().name().toLowerCase(Locale.ROOT) + " is " + economyService.format(minimum));
        }
        if (getListingsBySeller(seller.getUniqueId()).size() >= maxListingsPerPlayer && !seller.hasPermission("orinstoneauction.admin")) {
            throw new IllegalStateException("you reached your listing limit");
        }
        if (listingFee > 0.0D) {
            if (!economyService.has(seller, listingFee)) throw new IllegalStateException("you do not have enough money for the listing fee");
            if (!economyService.withdraw(seller, listingFee)) throw new IllegalStateException("failed to take the listing fee");
        }
        long createdAt = System.currentTimeMillis();
        long expiresAt = createdAt + (listingDurationHours * 60L * 60L * 1000L);
        long auctionId = repository.createAuction(seller.getUniqueId(), seller.getName(), itemStack.clone(), price, createdAt, expiresAt);
        plugin.getDiscordWebhookService().sendListingCreated(seller, itemStack, price, auctionId);
        return auctionId;
    }

    public double getMinimumPrice(Material material) {
        String path = "minimum-prices." + material.name();
        if (plugin.getConfig().isSet(path)) {
            return plugin.getConfig().getDouble(path, 0.0D);
        }
        return 0.0D;
    }

    public List<AuctionItem> getBrowsePage(int page, int pageSize) {
        List<AuctionItem> auctions = getActiveAuctions();
        int fromIndex = Math.max(0, page * pageSize);
        if (fromIndex >= auctions.size()) return List.of();
        int toIndex = Math.min(auctions.size(), fromIndex + pageSize);
        return auctions.subList(fromIndex, toIndex);
    }

    public int getBrowsePages(int pageSize) {
        int size = getActiveAuctions().size();
        return Math.max(1, (int) Math.ceil(size / (double) pageSize));
    }

    public List<AuctionItem> getActiveAuctions() {
        processExpiredAuctions();
        return repository.getAllActiveAuctions().stream().sorted(Comparator.comparingLong(AuctionItem::getCreatedAt).reversed()).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<AuctionItem> getListingsBySeller(UUID sellerUuid) {
        processExpiredAuctions();
        return repository.getAuctionsBySeller(sellerUuid).stream().sorted(Comparator.comparingLong(AuctionItem::getCreatedAt).reversed()).toList();
    }

    public boolean buy(Player buyer, long auctionId) {
        processExpiredAuctions();
        AuctionItem auctionItem = repository.findAuction(auctionId);
        if (auctionItem == null) throw new IllegalStateException("this listing is no longer available");
        if (auctionItem.getSellerUuid().equals(buyer.getUniqueId())) throw new IllegalStateException("you cannot buy your own listing");
        if (!economyService.has(buyer, auctionItem.getPrice())) throw new IllegalStateException("you do not have enough money");
        if (!economyService.withdraw(buyer, auctionItem.getPrice())) throw new IllegalStateException("payment failed");

        double tax = auctionItem.getPrice() * Math.max(0.0D, saleTaxPercent / 100.0D);
        double payout = Math.max(0.0D, auctionItem.getPrice() - tax);
        OfflinePlayer seller = Bukkit.getOfflinePlayer(auctionItem.getSellerUuid());
        if (!economyService.deposit(seller, payout)) {
            economyService.deposit(buyer, auctionItem.getPrice());
            throw new IllegalStateException("seller payout failed, payment was reverted");
        }

        repository.recordSale(auctionItem, buyer.getUniqueId(), buyer.getName(), tax, payout, System.currentTimeMillis());
        plugin.getDiscordWebhookService().sendListingSold(buyer, auctionItem, tax, payout);
        repository.deleteAuction(auctionId);
        giveOrDrop(buyer, auctionItem.getItemStack());
        return true;
    }

    public void cancelListing(Player seller, long auctionId) {
        AuctionItem auctionItem = repository.findAuction(auctionId);
        if (auctionItem == null) throw new IllegalStateException("this listing no longer exists");
        if (!auctionItem.getSellerUuid().equals(seller.getUniqueId()) && !seller.hasPermission("orinstoneauction.admin")) throw new IllegalStateException("you do not own this listing");
        repository.deleteAuction(auctionId);
        plugin.getDiscordWebhookService().sendListingCancelled(seller, auctionItem);
        giveOrDrop(seller, auctionItem.getItemStack());
    }

    public List<ExpiredClaim> getExpiredClaims(UUID sellerUuid) { return repository.getExpiredClaims(sellerUuid); }

    public List<SaleHistory> getSalesHistory(UUID playerUuid) {
        return repository.getSalesHistory(playerUuid, plugin.getConfig().getInt("history.limit", 45));
    }

    public void claimExpired(Player player, long claimId) {
        ExpiredClaim target = repository.getExpiredClaims(player.getUniqueId()).stream().filter(claim -> claim.getId() == claimId).findFirst().orElseThrow(() -> new IllegalStateException("expired item not found"));
        repository.claimExpired(claimId);
        giveOrDrop(player, target.getItemStack());
    }

    public void processExpiredAuctions() {
        long now = System.currentTimeMillis();
        for (AuctionItem auctionItem : repository.getAllActiveAuctions()) {
            if (auctionItem.getExpiresAt() <= now) {
                repository.deleteAuction(auctionItem.getId());
                repository.addExpiredClaim(auctionItem.getSellerUuid(), auctionItem.getItemStack(), auctionItem.getExpiresAt());
                plugin.getDiscordWebhookService().sendListingExpired(auctionItem);
            }
        }
    }

    public double getSaleTaxPercent() { return saleTaxPercent; }
    public double getListingFee() { return listingFee; }
    public int getMaxListingsPerPlayer() { return maxListingsPerPlayer; }

    private void giveOrDrop(Player player, ItemStack itemStack) {
        var leftovers = player.getInventory().addItem(itemStack.clone());
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
