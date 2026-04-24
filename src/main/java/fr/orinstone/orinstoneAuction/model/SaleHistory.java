package fr.orinstone.orinstoneAuction.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SaleHistory {
    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID buyerUuid;
    private final String buyerName;
    private final ItemStack itemStack;
    private final double price;
    private final double tax;
    private final double payout;
    private final long soldAt;

    public SaleHistory(long id, UUID sellerUuid, String sellerName, UUID buyerUuid, String buyerName, ItemStack itemStack, double price, double tax, double payout, long soldAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.itemStack = itemStack;
        this.price = price;
        this.tax = tax;
        this.payout = payout;
        this.soldAt = soldAt;
    }

    public long getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public UUID getBuyerUuid() { return buyerUuid; }
    public String getBuyerName() { return buyerName; }
    public ItemStack getItemStack() { return itemStack; }
    public double getPrice() { return price; }
    public double getTax() { return tax; }
    public double getPayout() { return payout; }
    public long getSoldAt() { return soldAt; }
}
