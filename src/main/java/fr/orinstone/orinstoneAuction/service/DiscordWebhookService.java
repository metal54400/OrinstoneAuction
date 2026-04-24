package fr.orinstone.orinstoneAuction.service;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import fr.orinstone.orinstoneAuction.model.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordWebhookService {
    private final OrinstoneAuctionPlugin plugin;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String url;
    private final String username;
    private final String avatarUrl;

    public DiscordWebhookService(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        this.enabled = plugin.getConfig().getBoolean("discord-webhook.enabled", false);
        this.url = plugin.getConfig().getString("discord-webhook.url", "");
        this.username = plugin.getConfig().getString("discord-webhook.username", "OrinstoneAuction");
        this.avatarUrl = plugin.getConfig().getString("discord-webhook.avatar-url", "");
    }

    public void sendListingCreated(Player seller, ItemStack itemStack, double price, long auctionId) {
        if (!plugin.getConfig().getBoolean("discord-webhook.events.listed", true)) return;
        Map<String, String> placeholders = basePlaceholders();
        placeholders.put("seller", seller.getName());
        placeholders.put("buyer", "");
        placeholders.put("item", prettyItemName(itemStack));
        placeholders.put("material", itemStack.getType().name());
        placeholders.put("amount", String.valueOf(itemStack.getAmount()));
        placeholders.put("price", plugin.getEconomyService().format(price));
        placeholders.put("auction_id", String.valueOf(auctionId));
        sendConfigured("listed", placeholders);
    }

    public void sendListingSold(Player buyer, AuctionItem auctionItem, double tax, double payout) {
        if (!plugin.getConfig().getBoolean("discord-webhook.events.sold", true)) return;
        Map<String, String> placeholders = basePlaceholders();
        placeholders.put("seller", auctionItem.getSellerName());
        placeholders.put("buyer", buyer.getName());
        placeholders.put("item", prettyItemName(auctionItem.getItemStack()));
        placeholders.put("material", auctionItem.getItemStack().getType().name());
        placeholders.put("amount", String.valueOf(auctionItem.getItemStack().getAmount()));
        placeholders.put("price", plugin.getEconomyService().format(auctionItem.getPrice()));
        placeholders.put("tax", plugin.getEconomyService().format(tax));
        placeholders.put("payout", plugin.getEconomyService().format(payout));
        placeholders.put("auction_id", String.valueOf(auctionItem.getId()));
        sendConfigured("sold", placeholders);
    }

    public void sendListingCancelled(Player seller, AuctionItem auctionItem) {
        if (!plugin.getConfig().getBoolean("discord-webhook.events.cancelled", false)) return;
        Map<String, String> placeholders = basePlaceholders();
        placeholders.put("seller", seller.getName());
        placeholders.put("buyer", "");
        placeholders.put("item", prettyItemName(auctionItem.getItemStack()));
        placeholders.put("material", auctionItem.getItemStack().getType().name());
        placeholders.put("amount", String.valueOf(auctionItem.getItemStack().getAmount()));
        placeholders.put("price", plugin.getEconomyService().format(auctionItem.getPrice()));
        placeholders.put("auction_id", String.valueOf(auctionItem.getId()));
        sendConfigured("cancelled", placeholders);
    }

    public void sendListingExpired(AuctionItem auctionItem) {
        if (!plugin.getConfig().getBoolean("discord-webhook.events.expired", false)) return;
        Map<String, String> placeholders = basePlaceholders();
        placeholders.put("seller", auctionItem.getSellerName());
        placeholders.put("buyer", "");
        placeholders.put("item", prettyItemName(auctionItem.getItemStack()));
        placeholders.put("material", auctionItem.getItemStack().getType().name());
        placeholders.put("amount", String.valueOf(auctionItem.getItemStack().getAmount()));
        placeholders.put("price", plugin.getEconomyService().format(auctionItem.getPrice()));
        placeholders.put("auction_id", String.valueOf(auctionItem.getId()));
        sendConfigured("expired", placeholders);
    }

    private Map<String, String> basePlaceholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("server", plugin.getServer().getName());
        return placeholders;
    }

    private void sendConfigured(String event, Map<String, String> placeholders) {
        String content = replace(plugin.getConfig().getString("discord-webhook.messages." + event + ".content", ""), placeholders);
        String title = replace(plugin.getConfig().getString("discord-webhook.messages." + event + ".title", ""), placeholders);
        String description = replace(plugin.getConfig().getString("discord-webhook.messages." + event + ".description", ""), placeholders);
        int color = plugin.getConfig().getInt("discord-webhook.messages." + event + ".color", 12058623);
        send(content, title, description, color);
    }

    private void send(String content, String title, String description, int color) {
        if (!enabled || url == null || url.isBlank() || url.contains("WEBHOOK_URL_HERE")) return;
        StringBuilder json = new StringBuilder("{");
        appendJsonField(json, "username", username, false);
        if (avatarUrl != null && !avatarUrl.isBlank()) appendJsonField(json, "avatar_url", avatarUrl, true);
        if (content != null && !content.isBlank()) appendJsonField(json, "content", content, true);
        if ((title != null && !title.isBlank()) || (description != null && !description.isBlank())) {
            if (json.length() > 1) json.append(',');
            json.append("\"embeds\":[{");
            boolean hasEmbedField = false;
            if (title != null && !title.isBlank()) {
                appendJsonField(json, "title", title, false);
                hasEmbedField = true;
            }
            if (description != null && !description.isBlank()) {
                appendJsonField(json, "description", description, hasEmbedField);
                hasEmbedField = true;
            }
            if (hasEmbedField) json.append(',');
            json.append("\"color\":").append(color).append("}]");
        }
        json.append('}');

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(throwable -> {
            plugin.getLogger().warning("Discord webhook failed: " + throwable.getMessage());
            return null;
        });
    }

    private void appendJsonField(StringBuilder json, String key, String value, boolean comma) {
        if (comma && json.charAt(json.length() - 1) != '{') json.append(',');
        json.append('"').append(escape(key)).append("\":\"").append(escape(value == null ? "" : value)).append('"');
    }

    private String replace(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private String prettyItemName(ItemStack itemStack) {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }
        String[] parts = itemStack.getType().name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
