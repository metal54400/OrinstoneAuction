package fr.orinstone.orinstoneAuction.listener;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import fr.orinstone.orinstoneAuction.gui.AuctionMenuHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener implements Listener {
    private final OrinstoneAuctionPlugin plugin;

    public InventoryListener(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof AuctionMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getMenuType()) {
            case BROWSE -> handleBrowseClick(player, holder, clicked, event.getRawSlot());
            case MY_LISTINGS -> handleMyListingsClick(player, holder, clicked, event.getRawSlot(), event.isShiftClick());
            case EXPIRED -> handleExpiredClick(player, holder, clicked, event.getRawSlot());
            case HISTORY -> handleHistoryClick(player, holder, clicked, event.getRawSlot());
        }
    }

    private void handleBrowseClick(Player player, AuctionMenuHolder holder, ItemStack clicked, int rawSlot) {
        Material type = clicked.getType();
        if (type == Material.ARROW) {
            if (rawSlot == 45) {
                plugin.getAuctionMenu().openBrowse(player, holder.getPage() - 1);
            } else {
                plugin.getAuctionMenu().openBrowse(player, holder.getPage() + 1);
            }
            return;
        }
        if (type == Material.CHEST) {
            plugin.getAuctionMenu().openMyListings(player, 0);
            return;
        }
        if (type == Material.HOPPER) {
            plugin.getAuctionMenu().openExpired(player, 0);
            return;
        }
        if (type == Material.PAPER) {
            plugin.getAuctionMenu().openHistory(player, 0);
            return;
        }

        Long auctionId = getLong(clicked, plugin.getAuctionIdKey());
        if (auctionId == null) {
            return;
        }
        try {
            plugin.getAuctionService().buy(player, auctionId);
            plugin.getMessageUtil().send(player, "&apurchase completed");
            playConfigured(player, "sounds.success", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            plugin.getAuctionMenu().openBrowse(player, holder.getPage());
        } catch (IllegalStateException exception) {
            plugin.getMessageUtil().send(player, "&c" + exception.getMessage());
            playConfigured(player, "sounds.fail", Sound.ENTITY_VILLAGER_NO);
        }
    }

    private void handleMyListingsClick(Player player, AuctionMenuHolder holder, ItemStack clicked, int rawSlot, boolean shiftClick) {
        Material type = clicked.getType();
        if (type == Material.ARROW) {
            if (rawSlot == 45) {
                plugin.getAuctionMenu().openMyListings(player, holder.getPage() - 1);
            } else {
                plugin.getAuctionMenu().openMyListings(player, holder.getPage() + 1);
            }
            return;
        }
        if (type == Material.BARRIER) {
            plugin.getAuctionMenu().openBrowse(player, 0);
            return;
        }
        Long auctionId = getLong(clicked, plugin.getAuctionIdKey());
        if (auctionId == null) {
            return;
        }
        if (!shiftClick) {
            plugin.getMessageUtil().send(player, "&eshift click to cancel your listing");
            return;
        }
        try {
            plugin.getAuctionService().cancelListing(player, auctionId);
            plugin.getMessageUtil().send(player, "&ayour listing was cancelled");
            playConfigured(player, "sounds.success", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            plugin.getAuctionMenu().openMyListings(player, holder.getPage());
        } catch (IllegalStateException exception) {
            plugin.getMessageUtil().send(player, "&c" + exception.getMessage());
            playConfigured(player, "sounds.fail", Sound.ENTITY_VILLAGER_NO);
        }
    }

    private void handleExpiredClick(Player player, AuctionMenuHolder holder, ItemStack clicked, int rawSlot) {
        Material type = clicked.getType();
        if (type == Material.ARROW) {
            if (rawSlot == 45) {
                plugin.getAuctionMenu().openExpired(player, holder.getPage() - 1);
            } else {
                plugin.getAuctionMenu().openExpired(player, holder.getPage() + 1);
            }
            return;
        }
        if (type == Material.BARRIER) {
            plugin.getAuctionMenu().openBrowse(player, 0);
            return;
        }
        Long claimId = getLong(clicked, plugin.getClaimIdKey());
        if (claimId == null) {
            return;
        }
        try {
            plugin.getAuctionService().claimExpired(player, claimId);
            plugin.getMessageUtil().send(player, "&aexpired item claimed");
            playConfigured(player, "sounds.success", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            plugin.getAuctionMenu().openExpired(player, holder.getPage());
        } catch (IllegalStateException exception) {
            plugin.getMessageUtil().send(player, "&c" + exception.getMessage());
            playConfigured(player, "sounds.fail", Sound.ENTITY_VILLAGER_NO);
        }
    }

    private void handleHistoryClick(Player player, AuctionMenuHolder holder, ItemStack clicked, int rawSlot) {
        Material type = clicked.getType();
        if (type == Material.ARROW) {
            if (rawSlot == 45) {
                plugin.getAuctionMenu().openHistory(player, holder.getPage() - 1);
            } else {
                plugin.getAuctionMenu().openHistory(player, holder.getPage() + 1);
            }
            return;
        }
        if (type == Material.BARRIER) {
            plugin.getAuctionMenu().openBrowse(player, 0);
        }
    }

    private Long getLong(ItemStack itemStack, NamespacedKey key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.LONG) ? container.get(key, PersistentDataType.LONG) : null;
    }

    private void playConfigured(Player player, String path, Sound fallback) {
        String soundName = plugin.getConfig().getString(path, fallback.name());
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0F, 1.0F);
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), fallback, 1.0F, 1.0F);
        }
    }
}
