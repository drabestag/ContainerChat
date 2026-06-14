package dev.containerchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ContainerChatListener implements Listener {

    private final ContainerChat plugin;

    public ContainerChatListener(ContainerChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Get the raw message text
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

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

        // Check permission
        if (!player.hasPermission("containerchat.use")) {
            player.sendMessage(ColorUtil.color("&cYou don't have permission to use container chat."));
            return;
        }

        // We must access the block on the main thread.
        // AsyncChatEvent runs async — schedule a sync task.
        final String trigger = matchedTrigger;
        plugin.getServer().getScheduler().runTask(plugin, () ->
                handleContainerDisplay(event, player, rawMessage, trigger));
    }

    private void handleContainerDisplay(AsyncChatEvent event, Player player,
                                        String rawMessage, String trigger) {
        double maxDistance = plugin.getConfig().getDouble("max-distance", 6.0);

        // Ray-cast to find the targeted block
        Block targetBlock = player.getTargetBlockExact((int) Math.ceil(maxDistance));

        if (targetBlock == null || !ContainerUtil.isContainer(targetBlock)) {
            player.sendMessage(ColorUtil.color(
                    plugin.getConfig().getString("no-target-message",
                            "&cYou are not looking at a container!")));
            // Cancel original message so the raw trigger text isn't sent
            event.setCancelled(true);
            return;
        }

        if (!ContainerUtil.isInRange(player, targetBlock, maxDistance)) {
            String msg = plugin.getConfig().getString("too-far-message",
                    "&cYou are too far from that container! (Max: {max} blocks)");
            msg = msg.replace("{max}", String.valueOf((int) maxDistance));
            player.sendMessage(ColorUtil.color(msg));
            event.setCancelled(true);
            return;
        }

        // --- Build the replacement message ---
        String containerType = ContainerUtil.getContainerTypeName(targetBlock);

        String headerFormat = plugin.getConfig().getString("header-format",
                "&8[&6{player}&8] &eLooking at &b{container_type}");
        String headerStr = headerFormat
                .replace("{player}", player.getName())
                .replace("{container_type}", containerType)
                .replace("{world}", targetBlock.getWorld().getName());

        // Build hover text from inventory contents
        Inventory inv = ContainerUtil.getContainerInventory(targetBlock);
        List<String> contentLines = ContainerUtil.buildContentLines(
                inv,
                plugin.getConfig().getString("item-format", "  &7• &f{amount}x &e{display_name}"),
                plugin.getConfig().getString("empty-format", "  &7(Empty container)"),
                plugin.getConfig().getBoolean("show-empty-slots", true)
        );

        // Build the hover component (shown when hovering the [container] link in chat)
        Component hoverHeader = ColorUtil.color("&b&l" + containerType)
                .append(Component.newline())
                .append(Component.newline());

        Component hoverBody = hoverHeader;
        for (String line : contentLines) {
            hoverBody = hoverBody.append(ColorUtil.color(line)).append(Component.newline());
        }

        // The clickable [container] tag in the message
        Component containerTag = Component.text("[" + containerType + "]")
                .color(TextColor.fromHexString("#5BC8F5"))
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(hoverBody));

        // Replace the trigger in the original message with the interactive component
        Component originalMessage = event.message();
        String plainText = PlainTextComponentSerializer.plainText().serialize(originalMessage);

        // Build the new message by splitting around the trigger
        Component newMessage = buildReplacedMessage(plainText, trigger.toLowerCase(), containerTag);

        // Cancel the original and resend with our modified message
        event.setCancelled(true);

        boolean broadcast = plugin.getConfig().getBoolean("broadcast", true);

        // Build the full chat line: header + the player's (modified) message
        Component fullHeader = ColorUtil.color(headerStr);

        Component chatLine = Component.empty()
                .append(fullHeader)
                .append(Component.newline())
                .append(Component.text(player.getName())
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(": ").color(NamedTextColor.GRAY))
                        .append(newMessage));

        if (broadcast) {
            // Send to all players + console
            plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(chatLine));
            plugin.getServer().getConsoleSender().sendMessage(chatLine);
        } else {
            // Only sender sees it
            player.sendMessage(chatLine);
            plugin.getServer().getConsoleSender().sendMessage(chatLine);
        }
    }

    /**
     * Splits the plain-text message on the first occurrence of the trigger (case-insensitive)
     * and inserts the interactive component in its place.
     */
    private Component buildReplacedMessage(String plain, String triggerLower, Component replacement) {
        int idx = plain.toLowerCase().indexOf(triggerLower);
        if (idx < 0) {
            return Component.text(plain).color(NamedTextColor.WHITE);
        }

        Component before = Component.text(plain.substring(0, idx)).color(NamedTextColor.WHITE);
        Component after  = Component.text(plain.substring(idx + triggerLower.length())).color(NamedTextColor.WHITE);

        return before.append(replacement).append(after);
    }
}
