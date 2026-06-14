package dev.containerchat;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for inspecting containers and producing chat lines.
 */
public final class ContainerUtil {

    private ContainerUtil() {}

    /**
     * Returns true if the given block is a supported container type.
     */
    public static boolean isContainer(Block block) {
        if (block == null) return false;
        BlockState state = block.getState();
        return state instanceof Container;
    }

    /**
     * Returns a friendly display name for the container type.
     */
    public static String getContainerTypeName(Block block) {
        BlockState state = block.getState();

        if (state instanceof ShulkerBox) {
            // Include shulker colour
            String colour = block.getType().name()
                    .replace("_SHULKER_BOX", "")
                    .replace("_", " ");
            if (colour.equalsIgnoreCase("shulker box")) {
                return "Shulker Box";
            }
            return toTitleCase(colour) + " Shulker Box";
        }

        // Fallback: prettify the block type name
        return toTitleCase(block.getType().name().replace("_", " "));
    }

    /**
     * Returns the inventory of the targeted container block.
     * Handles double chests by returning the full double-chest inventory.
     */
    public static Inventory getContainerInventory(Block block) {
        BlockState state = block.getState();
        if (state instanceof Container container) {
            return container.getInventory();
        }
        return null;
    }

    /**
     * Checks whether the player is within maxDistance blocks of the block.
     */
    public static boolean isInRange(Player player, Block block, double maxDistance) {
        return player.getLocation().distanceSquared(block.getLocation().toCenterLocation())
                <= maxDistance * maxDistance;
    }

    /**
     * Builds the list of chat component strings describing the container contents.
     * Each returned string may contain &-colour codes.
     */
    public static List<String> buildContentLines(Inventory inventory,
                                                  String itemFormat,
                                                  String emptyFormat,
                                                  boolean showEmptySlots) {
        List<String> lines = new ArrayList<>();
        int emptySlots = 0;
        boolean hasItems = false;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                emptySlots++;
                continue;
            }
            hasItems = true;
            String displayName = getDisplayName(item);
            String line = itemFormat
                    .replace("{amount}", String.valueOf(item.getAmount()))
                    .replace("{item}", toTitleCase(item.getType().name().replace("_", " ")))
                    .replace("{display_name}", displayName);
            lines.add(line);
        }

        if (!hasItems) {
            lines.add(emptyFormat);
        } else if (showEmptySlots && emptySlots > 0) {
            lines.add("  &7(" + emptySlots + " empty slot" + (emptySlots != 1 ? "s" : "") + ")");
        }

        return lines;
    }

    /**
     * Returns the display name of an item, or its material name if it has no custom name.
     */
    private static String getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // Strip legacy colour from display name so we can re-colour it our way
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection()
                    .serialize(meta.displayName())
                    .replaceAll("§.", ""); // Strip colour codes from custom names for clean display
        }
        return toTitleCase(item.getType().name().replace("_", " "));
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
