package fr.orinstone.orinstoneAuction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AuctionMenuHolder implements InventoryHolder {
    private final MenuType menuType;
    private final int page;
    private Inventory inventory;

    public AuctionMenuHolder(MenuType menuType, int page) {
        this.menuType = menuType;
        this.page = page;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
