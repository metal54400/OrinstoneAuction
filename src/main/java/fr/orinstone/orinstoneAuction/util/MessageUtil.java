package fr.orinstone.orinstoneAuction.util;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MessageUtil {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private final OrinstoneAuctionPlugin plugin;
    private final String prefix;
    private final boolean smallCaps;

    public MessageUtil(OrinstoneAuctionPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.prefix = config.getString("messages.prefix", "");
        this.smallCaps = config.getBoolean("messages.use-small-caps", true);
    }

    public void send(Player player, String message) {
        player.sendMessage(component(player, prefix + message));
    }

    public Component component(String raw) {
        return component(null, raw);
    }

    public Component component(Player player, String raw) {
        String withPlaceholders = plugin.getPlaceholderService().apply(player, raw);
        String processed = smallCaps ? toSmallCapsPreservingColorCodes(withPlaceholders) : withPlaceholders;
        return SERIALIZER.deserialize(processed);
    }

    public String plain(Player player, String raw) {
        String withPlaceholders = plugin.getPlaceholderService().apply(player, raw);
        return smallCaps ? toSmallCapsPreservingColorCodes(withPlaceholders) : withPlaceholders;
    }

    public List<Component> lore(Player player, List<String> lines) {
        List<Component> output = new ArrayList<>();
        for (String line : lines) {
            output.add(component(player, line));
        }
        return output;
    }

    public List<Component> lore(List<String> lines) {
        return lore(null, lines);
    }

    private String toSmallCapsPreservingColorCodes(String input) {
        StringBuilder builder = new StringBuilder();
        boolean colorMarker = false;
        for (char character : input.toCharArray()) {
            if (colorMarker) {
                builder.append(character);
                colorMarker = false;
                continue;
            }
            if (character == '&' || character == '§') {
                builder.append(character);
                colorMarker = true;
                continue;
            }
            builder.append(SmallCapsUtil.transform(String.valueOf(character)));
        }
        return builder.toString();
    }
}
