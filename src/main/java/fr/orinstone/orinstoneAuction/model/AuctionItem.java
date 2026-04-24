package fr.orinstone.orinstoneAuction.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {
    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    public AuctionItem(long id, UUID sellerUuid, String sellerName, ItemStack itemStack, double price, long createdAt, long expiresAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
