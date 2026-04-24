package fr.orinstone.orinstoneAuction.storage;

import fr.orinstone.orinstoneAuction.OrinstoneAuctionPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteManager {
    private final String jdbcUrl;

    public SQLiteManager(OrinstoneAuctionPlugin plugin) {
        String fileName = plugin.getConfig().getString("storage.file", "auctions.db");
        File file = new File(plugin.getDataFolder(), fileName);
        this.jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
    }

    public void init() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item_base64 TEXT NOT NULL,
                    price DOUBLE NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL
                )
            """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS expired_claims (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    item_base64 TEXT NOT NULL,
                    expired_at BIGINT NOT NULL,
                    claimed INTEGER NOT NULL DEFAULT 0
                )
            """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sales_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    buyer_uuid TEXT NOT NULL,
                    buyer_name TEXT NOT NULL,
                    item_base64 TEXT NOT NULL,
                    price DOUBLE NOT NULL,
                    tax DOUBLE NOT NULL,
                    payout DOUBLE NOT NULL,
                    sold_at BIGINT NOT NULL
                )
            """);
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to initialize sqlite", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
