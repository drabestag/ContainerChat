package dev.containerchat;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ContainerUtil {

    private ContainerUtil() {}

    public static boolean isContainer(Block block) {
        if (block == null) return false;
        return block.getState() instanceof Container;
    }

    public static String getContainerTypeName(Block block) {
        BlockState state = block.getState();
        if (state instanceof ShulkerBox) {
            String colour = block.getType().name()
                    .replace("_SHULKER_BOX", "")
                    .replace("_", " ");
            if (colour.equalsIgnoreCase("shulker box")) return "Shulker Box";
            return toTitleCase(colour) + " Shulker Box";
        }
        return toTitleCase(block.getType().name().replace("_", " "));
    }

    public static Inventory getContainerInventory(Block block) {
        BlockState state = block.getState();
        if (state instanceof Container container) {
            return container.getInventory();
        }
        return null;
    }

    public static boolean isInRange(Player player, Block block, double maxDistance) {
        return player.getLocation().distanceSquared(block.getLocation().toCenterLocation())
                <= maxDistance * maxDistance;
    }

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

    private static String getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // Strip colour codes from custom names for clean display
            return meta.getDisplayName().replaceAll("§.", "");
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
