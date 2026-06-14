package dev.containerchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {

    private ColorUtil() {}

    /** Translate &-colour codes into a Component. */
    public static Component color(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    /** Translate &-colour codes into a plain coloured string (for legacy contexts). */
    public static String colorString(String message) {
        return message.replace("&", "§");
    }
}
