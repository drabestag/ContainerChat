package dev.containerchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ContainerChat extends JavaPlugin implements CommandExecutor {

    private ContainerChatListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        listener = new ContainerChatListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        var cmd = getCommand("containerchat");
        if (cmd != null) cmd.setExecutor(this);

        getLogger().info("ContainerChat (Spigot) enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ContainerChat disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("containerchat.reload")) {
                sender.sendMessage(ColorUtil.colorString("&cYou don't have permission to do that."));
                return true;
            }
            reloadConfig();
            sender.sendMessage(ColorUtil.colorString("&aContainerChat config reloaded!"));
            return true;
        }
        sender.sendMessage(ColorUtil.colorString("&eContainerChat &7v" + getDescription().getVersion()
                + " &8| &7/containerchat reload"));
        return true;
    }
}
