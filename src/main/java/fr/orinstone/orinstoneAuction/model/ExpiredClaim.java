package fr.orinstone.orinstoneAuction.model;

import org.bukkit.inventory.ItemStack;

public class ExpiredClaim {
    private final long id;
    private final ItemStack itemStack;
    private final long expiredAt;

    public ExpiredClaim(long id, ItemStack itemStack, long expiredAt) {
        this.id = id;
        this.itemStack = itemStack;
        this.expiredAt = expiredAt;
    }

    public long getId() {
        return id;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public long getExpiredAt() {
        return expiredAt;
    }
}
