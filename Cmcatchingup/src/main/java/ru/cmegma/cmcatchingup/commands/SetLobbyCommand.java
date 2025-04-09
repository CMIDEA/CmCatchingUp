package ru.cmegma.cmcatchingup.commands;

import ru.cmegma.cmcatchingup.Cmcatchingup;
import ru.cmegma.cmcatchingup.manager.ConfigManager;
import ru.cmegma.cmcatchingup.manager.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetLobbyCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MessagesManager messagesManager;

    public SetLobbyCommand(Cmcatchingup plugin) {
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messagesManager.getSimpleMessage("error.players-only"));
            return true;
        }

        if (!player.hasPermission("cmcatchingup.admin")) {
            player.sendMessage(messagesManager.getSimpleMessage("error.no-permission"));
            return true;
        }

        configManager.setLobbyLocation(player.getLocation());
        player.sendMessage(messagesManager.getSimpleMessage("command.setlobby.success"));
        return true;
    }
}