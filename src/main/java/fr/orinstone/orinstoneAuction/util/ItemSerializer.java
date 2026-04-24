package fr.orinstone.orinstoneAuction.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ItemSerializer {
    private ItemSerializer() {}

    public static String toBase64(ItemStack itemStack) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                outputStream.writeObject(itemStack);
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to serialize item", exception);
        }
    }

    public static ItemStack fromBase64(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream inputStream = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (ItemStack) inputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("failed to deserialize item", exception);
        }
    }
}
