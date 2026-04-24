package fr.orinstone.orinstoneAuction;

import fr.orinstone.orinstoneAuction.command.AhCommand;
import fr.orinstone.orinstoneAuction.command.AhSellCommand;
import fr.orinstone.orinstoneAuction.gui.AuctionMenu;
import fr.orinstone.orinstoneAuction.listener.InventoryListener;
import fr.orinstone.orinstoneAuction.service.AuctionService;
import fr.orinstone.orinstoneAuction.service.EconomyService;
import fr.orinstone.orinstoneAuction.service.DiscordLinkService;
import fr.orinstone.orinstoneAuction.service.DiscordWebhookService;
import fr.orinstone.orinstoneAuction.service.PlaceholderService;
import fr.orinstone.orinstoneAuction.storage.AuctionRepository;
import fr.orinstone.orinstoneAuction.storage.SQLiteManager;
import fr.orinstone.orinstoneAuction.util.MessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OrinstoneAuctionPlugin extends JavaPlugin {
    private MessageUtil messageUtil;
    private EconomyService economyService;
    private AuctionService auctionService;
    private AuctionMenu auctionMenu;
    private AuctionRepository auctionRepository;
    private PlaceholderService placeholderService;
    private DiscordLinkService discordLinkService;
    private DiscordWebhookService discordWebhookService;
    private NamespacedKey auctionIdKey;
    private NamespacedKey claimIdKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.auctionIdKey = new NamespacedKey(this, "auction_id");
        this.claimIdKey = new NamespacedKey(this, "claim_id");

        this.economyService = new EconomyService(this);
        if (!economyService.hook()) {
            getLogger().severe("Vault or a Vault economy provider was not found. disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SQLiteManager sqliteManager = new SQLiteManager(this);
        sqliteManager.init();
        this.auctionRepository = new AuctionRepository(sqliteManager);
        reloadComponents();

        var ahCommand = new AhCommand(this);
        var ahSellCommand = new AhSellCommand(this);
        PluginCommand ah = getCommand("ah");
        if (ah != null) {
            ah.setExecutor(ahCommand);
            ah.setTabCompleter(ahCommand);
        }
        PluginCommand ahSell = getCommand("ahsell");
        if (ahSell != null) {
            ahSell.setExecutor(ahSellCommand);
            ahSell.setTabCompleter(ahSellCommand);
        }

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, auctionService::processExpiredAuctions, 20L * 60L, 20L * 60L);

        getLogger().info("OrinstoneAuction enabled. PlaceholderAPI: " + (placeholderService.isEnabled() ? "enabled" : "disabled") + ", DiscordSRV: " + (discordLinkService.isDiscordSrvAvailable() ? "enabled" : "disabled"));
    }

    public MessageUtil getMessageUtil() { return messageUtil; }
    public EconomyService getEconomyService() { return economyService; }
    public AuctionService getAuctionService() { return auctionService; }
    public AuctionMenu getAuctionMenu() { return auctionMenu; }
    public PlaceholderService getPlaceholderService() { return placeholderService; }
    public DiscordLinkService getDiscordLinkService() { return discordLinkService; }
    public DiscordWebhookService getDiscordWebhookService() { return discordWebhookService; }
    public NamespacedKey getAuctionIdKey() { return auctionIdKey; }
    public NamespacedKey getClaimIdKey() { return claimIdKey; }

    public void reloadComponents() {
        reloadConfig();
        this.placeholderService = new PlaceholderService(this);
        this.discordLinkService = new DiscordLinkService(this);
        this.discordWebhookService = new DiscordWebhookService(this);
        this.messageUtil = new MessageUtil(this);
        this.auctionService = new AuctionService(this, auctionRepository, economyService);
        this.auctionMenu = new AuctionMenu(this, messageUtil);
    }
}
