package dev.containerchat;

import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {}

    /** Translate &-colour codes into a Bukkit-coloured string. */
    public static String colorString(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
