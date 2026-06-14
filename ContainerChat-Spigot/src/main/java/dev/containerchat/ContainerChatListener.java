package dev.containerchat;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class ContainerChatListener implements Listener {

    private final ContainerChat plugin;

    public ContainerChatListener(ContainerChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();

        // Check if any trigger is present (case-insensitive)
        List<String> triggers = plugin.getConfig().getStringList("triggers");
        boolean hasTrigger = false;
        String matchedTrigger = null;
        String lowerMessage = rawMessage.toLowerCase();

        for (String trigger : triggers) {
            if (lowerMessage.contains(trigger.toLowerCase())) {
                hasTrigger = true;
                matchedTrigger = trigger;
                break;
            }
        }

        if (!hasTrigger) return;

        if (!player.hasPermission("containerchat.use")) {
            player.sendMessage(ColorUtil.colorString("&cYou don't have permission to use container chat."));
            return;
        }

        // Block access must happen on the main thread
        final String trigger = matchedTrigger;
        plugin.getServer().getScheduler().runTask(plugin, () ->
                handleContainerDisplay(event, player, rawMessage, trigger));
    }

    private void handleContainerDisplay(AsyncPlayerChatEvent event, Player player,
                                        String rawMessage, String trigger) {
        double maxDistance = plugin.getConfig().getDouble("max-distance", 6.0);

        Block targetBlock = player.getTargetBlockExact((int) Math.ceil(maxDistance));

        if (targetBlock == null || !ContainerUtil.isContainer(targetBlock)) {
            player.sendMessage(ColorUtil.colorString(
                    plugin.getConfig().getString("no-target-message",
                            "&cYou are not looking at a container!")));
            event.setCancelled(true);
            return;
        }

        if (!ContainerUtil.isInRange(player, targetBlock, maxDistance)) {
            String msg = plugin.getConfig().getString("too-far-message",
                    "&cYou are too far from that container! (Max: {max} blocks)");
            msg = msg.replace("{max}", String.valueOf((int) maxDistance));
            player.sendMessage(ColorUtil.colorString(msg));
            event.setCancelled(true);
            return;
        }

        String containerType = ContainerUtil.getContainerTypeName(targetBlock);

        String headerFormat = plugin.getConfig().getString("header-format",
                "&8[&6{player}&8] &eLooking at &b{container_type}");
        String header = ColorUtil.colorString(headerFormat
                .replace("{player}", player.getName())
                .replace("{container_type}", containerType)
                .replace("{world}", targetBlock.getWorld().getName()));

        // Build inventory content lines
        Inventory inv = ContainerUtil.getContainerInventory(targetBlock);
        List<String> contentLines = ContainerUtil.buildContentLines(
                inv,
                plugin.getConfig().getString("item-format", "  &7• &f{amount}x &e{display_name}"),
                plugin.getConfig().getString("empty-format", "  &7(Empty container)"),
                plugin.getConfig().getBoolean("show-empty-slots", true)
        );

        // Replace the trigger in the message with a coloured [ContainerType] label
        String containerTag = ColorUtil.colorString("&b&l[" + containerType + "]&r");
        String modifiedMessage = replaceIgnoreCase(rawMessage, trigger, containerTag);

        // Build the full output — header, then the chat line
        StringBuilder output = new StringBuilder();
        output.append(header).append("\n");
        output.append(ColorUtil.colorString("&e")).append(player.getName())
              .append(ColorUtil.colorString("&7: ")).append(modifiedMessage).append("\n");
        for (String line : contentLines) {
            output.append(ColorUtil.colorString(line)).append("\n");
        }

        String finalOutput = output.toString().stripTrailing();

        event.setCancelled(true);

        boolean broadcast = plugin.getConfig().getBoolean("broadcast", true);

        if (broadcast) {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(finalOutput));
            plugin.getServer().getConsoleSender().sendMessage(finalOutput);
        } else {
            player.sendMessage(finalOutput);
            plugin.getServer().getConsoleSender().sendMessage(finalOutput);
        }
    }

    /** Case-insensitive string replacement for the first occurrence of target. */
    private String replaceIgnoreCase(String original, String target, String replacement) {
        int idx = original.toLowerCase().indexOf(target.toLowerCase());
        if (idx < 0) return original;
        return original.substring(0, idx) + replacement + original.substring(idx + target.length());
    }
}
