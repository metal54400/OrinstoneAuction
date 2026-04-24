package fr.orinstone.orinstoneAuction.gui;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import fr.orinstone.orinstoneAuction.model.AuctionItem;
import fr.orinstone.orinstoneAuction.model.ExpiredClaim;
import fr.orinstone.orinstoneAuction.model.SaleHistory;
import fr.orinstone.orinstoneAuction.util.MessageUtil;
import fr.orinstone.orinstoneAuction.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AuctionMenu {
    private final OrinstoneAuctionPlugin plugin;
    private final MessageUtil messages;
    private final int pageSize;

    public AuctionMenu(OrinstoneAuctionPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.pageSize = plugin.getConfig().getInt("auction.browse-page-size", 45);
    }

    public void openBrowse(Player player, int requestedPage) {
        int pages = plugin.getAuctionService().getBrowsePages(pageSize);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.BROWSE, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.component(player, plugin.getConfig().getString("gui.titles.browse", "&5auction house")));
        holder.setInventory(inventory);

        List<AuctionItem> auctions = plugin.getAuctionService().getBrowsePage(page, pageSize);
        for (int i = 0; i < auctions.size(); i++) inventory.setItem(i, createAuctionItem(player, auctions.get(i)));

        inventory.setItem(45, nav(player, Material.ARROW, "&dprevious page"));
        inventory.setItem(47, nav(player, Material.CHEST, "&dmy listings"));
        inventory.setItem(48, nav(player, Material.PAPER, "&dhistory"));
        inventory.setItem(49, info(player, Material.BOOK, "&dauction info", List.of(
            "&7active listings: &f" + plugin.getAuctionService().getActiveAuctions().size(),
            "&7sale tax: &f" + plugin.getAuctionService().getSaleTaxPercent() + "%",
            "&7listing fee: &f" + plugin.getEconomyService().format(plugin.getAuctionService().getListingFee())
        )));
        inventory.setItem(51, nav(player, Material.HOPPER, "&dexpired items"));
        inventory.setItem(53, nav(player, Material.ARROW, "&dnext page"));
        player.openInventory(inventory);
    }

    public void openMyListings(Player player, int requestedPage) {
        List<AuctionItem> listings = plugin.getAuctionService().getListingsBySeller(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) pageSize));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.MY_LISTINGS, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.component(player, plugin.getConfig().getString("gui.titles.my-listings", "&5my listings")));
        holder.setInventory(inventory);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(listings.size(), fromIndex + pageSize);
        List<AuctionItem> pageItems = listings.subList(fromIndex, toIndex);
        for (int i = 0; i < pageItems.size(); i++) inventory.setItem(i, createOwnAuctionItem(player, pageItems.get(i)));

        inventory.setItem(45, nav(player, Material.ARROW, "&dprevious page"));
        inventory.setItem(49, nav(player, Material.BARRIER, "&dback to auction house"));
        inventory.setItem(53, nav(player, Material.ARROW, "&dnext page"));
        player.openInventory(inventory);
    }

    public void openExpired(Player player, int requestedPage) {
        List<ExpiredClaim> claims = plugin.getAuctionService().getExpiredClaims(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(claims.size() / (double) pageSize));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.EXPIRED, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.component(player, plugin.getConfig().getString("gui.titles.expired", "&5expired items")));
        holder.setInventory(inventory);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(claims.size(), fromIndex + pageSize);
        List<ExpiredClaim> pageItems = claims.subList(fromIndex, toIndex);
        for (int i = 0; i < pageItems.size(); i++) inventory.setItem(i, createExpiredItem(player, pageItems.get(i)));

        inventory.setItem(45, nav(player, Material.ARROW, "&dprevious page"));
        inventory.setItem(49, nav(player, Material.BARRIER, "&dback to auction house"));
        inventory.setItem(53, nav(player, Material.ARROW, "&dnext page"));
        player.openInventory(inventory);
    }

    public void openHistory(Player player, int requestedPage) {
        List<SaleHistory> history = plugin.getAuctionService().getSalesHistory(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(history.size() / (double) pageSize));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.HISTORY, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.component(player, plugin.getConfig().getString("gui.titles.history", "&5sales history")));
        holder.setInventory(inventory);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(history.size(), fromIndex + pageSize);
        List<SaleHistory> pageItems = history.subList(fromIndex, toIndex);
        for (int i = 0; i < pageItems.size(); i++) inventory.setItem(i, createHistoryItem(player, pageItems.get(i)));

        inventory.setItem(45, nav(player, Material.ARROW, "&dprevious page"));
        inventory.setItem(49, nav(player, Material.BARRIER, "&dback to auction house"));
        inventory.setItem(53, nav(player, Material.ARROW, "&dnext page"));
        player.openInventory(inventory);
    }

    private ItemStack createAuctionItem(Player viewer, AuctionItem auctionItem) {
        ItemStack display = auctionItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (meta.lore() != null) for (Component component : meta.lore()) lore.add(PlainTextComponentSerializer.plainText().serialize(component));
            lore.add("");
            lore.add("&7seller: &f" + auctionItem.getSellerName());
            lore.add("&7price: &a" + plugin.getEconomyService().format(auctionItem.getPrice()));
            lore.add("&7time left: &f" + TimeUtil.formatRemaining(auctionItem.getExpiresAt() - System.currentTimeMillis()));
            lore.add("&eleft click to buy");
            meta.lore(messages.lore(viewer, lore));
            meta.getPersistentDataContainer().set(plugin.getAuctionIdKey(), PersistentDataType.LONG, auctionItem.getId());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createOwnAuctionItem(Player viewer, AuctionItem auctionItem) {
        ItemStack display = createAuctionItem(viewer, auctionItem);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(messages.component(viewer, "&cshift left click to cancel"));
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createExpiredItem(Player viewer, ExpiredClaim claim) {
        ItemStack display = claim.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&7expired: &f" + TimeUtil.formatRemaining(System.currentTimeMillis() - claim.getExpiredAt()) + " ago");
            lore.add("&eleft click to claim");
            meta.lore(messages.lore(viewer, lore));
            meta.getPersistentDataContainer().set(plugin.getClaimIdKey(), PersistentDataType.LONG, claim.getId());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createHistoryItem(Player viewer, SaleHistory sale) {
        ItemStack display = sale.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            boolean sellerView = sale.getSellerUuid().equals(viewer.getUniqueId());
            lore.add("");
            lore.add(sellerView ? "&a sold by you" : "&b bought by you");
            lore.add("&7seller: &f" + sale.getSellerName());
            lore.add("&7buyer: &f" + sale.getBuyerName());
            lore.add("&7price: &a" + plugin.getEconomyService().format(sale.getPrice()));
            lore.add("&7tax: &c" + plugin.getEconomyService().format(sale.getTax()));
            lore.add("&7payout: &a" + plugin.getEconomyService().format(sale.getPayout()));
            lore.add("&7sold: &f" + TimeUtil.formatRemaining(System.currentTimeMillis() - sale.getSoldAt()) + " ago");
            meta.lore(messages.lore(viewer, lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack nav(Player player, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player, name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack info(Player player, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component(player, name));
            meta.lore(messages.lore(player, lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
