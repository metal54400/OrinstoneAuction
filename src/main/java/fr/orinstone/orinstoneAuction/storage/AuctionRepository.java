package fr.orinstone.orinstoneAuction.storage;

import fr.orinstone.orinstoneAuction.model.AuctionItem;
import fr.orinstone.orinstoneAuction.model.ExpiredClaim;
import fr.orinstone.orinstoneAuction.model.SaleHistory;
import fr.orinstone.orinstoneAuction.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionRepository {
    private final SQLiteManager sqliteManager;

    public AuctionRepository(SQLiteManager sqliteManager) {
        this.sqliteManager = sqliteManager;
    }

    public long createAuction(UUID sellerUuid, String sellerName, ItemStack itemStack, double price, long createdAt, long expiresAt) {
        String sql = "INSERT INTO auctions (seller_uuid, seller_name, item_base64, price, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, sellerUuid.toString());
            statement.setString(2, sellerName);
            statement.setString(3, ItemSerializer.toBase64(itemStack));
            statement.setDouble(4, price);
            statement.setLong(5, createdAt);
            statement.setLong(6, expiresAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new SQLException("no generated key");
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to create auction", exception);
        }
    }

    public List<AuctionItem> getAllActiveAuctions() {
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        List<AuctionItem> output = new ArrayList<>();
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) output.add(mapAuction(resultSet));
            return output;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to read auctions", exception);
        }
    }

    public List<AuctionItem> getAuctionsBySeller(UUID sellerUuid) {
        String sql = "SELECT * FROM auctions WHERE seller_uuid = ? ORDER BY created_at DESC";
        List<AuctionItem> output = new ArrayList<>();
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) output.add(mapAuction(resultSet));
            }
            return output;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to read seller auctions", exception);
        }
    }

    public AuctionItem findAuction(long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapAuction(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to find auction", exception);
        }
    }

    public void deleteAuction(long id) {
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM auctions WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to delete auction", exception);
        }
    }

    public void addExpiredClaim(UUID sellerUuid, ItemStack itemStack, long expiredAt) {
        String sql = "INSERT INTO expired_claims (seller_uuid, item_base64, expired_at, claimed) VALUES (?, ?, ?, 0)";
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerUuid.toString());
            statement.setString(2, ItemSerializer.toBase64(itemStack));
            statement.setLong(3, expiredAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to add expired claim", exception);
        }
    }

    public List<ExpiredClaim> getExpiredClaims(UUID sellerUuid) {
        String sql = "SELECT * FROM expired_claims WHERE seller_uuid = ? AND claimed = 0 ORDER BY expired_at DESC";
        List<ExpiredClaim> output = new ArrayList<>();
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) output.add(new ExpiredClaim(resultSet.getLong("id"), ItemSerializer.fromBase64(resultSet.getString("item_base64")), resultSet.getLong("expired_at")));
            }
            return output;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to read expired claims", exception);
        }
    }

    public void claimExpired(long claimId) {
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE expired_claims SET claimed = 1 WHERE id = ?")) {
            statement.setLong(1, claimId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to claim expired item", exception);
        }
    }

    public void recordSale(AuctionItem auctionItem, UUID buyerUuid, String buyerName, double tax, double payout, long soldAt) {
        String sql = "INSERT INTO sales_history (seller_uuid, seller_name, buyer_uuid, buyer_name, item_base64, price, tax, payout, sold_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionItem.getSellerUuid().toString());
            statement.setString(2, auctionItem.getSellerName());
            statement.setString(3, buyerUuid.toString());
            statement.setString(4, buyerName);
            statement.setString(5, ItemSerializer.toBase64(auctionItem.getItemStack()));
            statement.setDouble(6, auctionItem.getPrice());
            statement.setDouble(7, tax);
            statement.setDouble(8, payout);
            statement.setLong(9, soldAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to record sale", exception);
        }
    }

    public List<SaleHistory> getSalesHistory(UUID playerUuid, int limit) {
        String sql = "SELECT * FROM sales_history WHERE seller_uuid = ? OR buyer_uuid = ? ORDER BY sold_at DESC LIMIT ?";
        List<SaleHistory> output = new ArrayList<>();
        try (Connection connection = sqliteManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerUuid.toString());
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) output.add(mapHistory(resultSet));
            }
            return output;
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to read sales history", exception);
        }
    }

    private AuctionItem mapAuction(ResultSet resultSet) throws SQLException {
        return new AuctionItem(resultSet.getLong("id"), UUID.fromString(resultSet.getString("seller_uuid")), resultSet.getString("seller_name"), ItemSerializer.fromBase64(resultSet.getString("item_base64")), resultSet.getDouble("price"), resultSet.getLong("created_at"), resultSet.getLong("expires_at"));
    }

    private SaleHistory mapHistory(ResultSet resultSet) throws SQLException {
        return new SaleHistory(resultSet.getLong("id"), UUID.fromString(resultSet.getString("seller_uuid")), resultSet.getString("seller_name"), UUID.fromString(resultSet.getString("buyer_uuid")), resultSet.getString("buyer_name"), ItemSerializer.fromBase64(resultSet.getString("item_base64")), resultSet.getDouble("price"), resultSet.getDouble("tax"), resultSet.getDouble("payout"), resultSet.getLong("sold_at"));
    }
}
