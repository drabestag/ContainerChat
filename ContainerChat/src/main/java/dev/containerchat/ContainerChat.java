package dev.containerchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ContainerChat extends JavaPlugin implements CommandExecutor {

    private ContainerChatListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        listener = new ContainerChatListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        var cmd = getCommand("containerchat");
        if (cmd != null) cmd.setExecutor(this);

        getLogger().info("ContainerChat enabled! Players can type [container] or [c] in chat.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ContainerChat disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("containerchat.reload")) {
                sender.sendMessage(ColorUtil.color("&cYou don't have permission to do that."));
                return true;
            }
            reloadConfig();
            sender.sendMessage(ColorUtil.color("&aContainerChat config reloaded!"));
            return true;
        }
        sender.sendMessage(ColorUtil.color("&eContainerChat &7v" + getDescription().getVersion()
                + " &8| &7/containerchat reload"));
        return true;
    }
}
