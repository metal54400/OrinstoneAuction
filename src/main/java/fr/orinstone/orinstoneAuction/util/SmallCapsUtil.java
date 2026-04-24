package fr.orinstone.orinstoneAuction.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SmallCapsUtil {
    private static final Map<Character, String> MAP = new HashMap<>();

    static {
        MAP.put('a', "ᴀ");
        MAP.put('b', "ʙ");
        MAP.put('c', "ᴄ");
        MAP.put('d', "ᴅ");
        MAP.put('e', "ᴇ");
        MAP.put('f', "ꜰ");
        MAP.put('g', "ɢ");
        MAP.put('h', "ʜ");
        MAP.put('i', "ɪ");
        MAP.put('j', "ᴊ");
        MAP.put('k', "ᴋ");
        MAP.put('l', "ʟ");
        MAP.put('m', "ᴍ");
        MAP.put('n', "ɴ");
        MAP.put('o', "ᴏ");
        MAP.put('p', "ᴘ");
        MAP.put('q', "ǫ");
        MAP.put('r', "ʀ");
        MAP.put('s', "ꜱ");
        MAP.put('t', "ᴛ");
        MAP.put('u', "ᴜ");
        MAP.put('v', "ᴠ");
        MAP.put('w', "ᴡ");
        MAP.put('x', "x");
        MAP.put('y', "ʏ");
        MAP.put('z', "ᴢ");
    }

    private SmallCapsUtil() {}

    public static String transform(String input) {
        StringBuilder builder = new StringBuilder();
        for (char character : input.toLowerCase(Locale.ROOT).toCharArray()) {
            builder.append(MAP.getOrDefault(character, String.valueOf(character)));
        }
        return builder.toString();
    }
}
