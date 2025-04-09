package ru.cmegma.cmcatchingup.commands;

import ru.cmegma.cmcatchingup.Cmcatchingup;
import ru.cmegma.cmcatchingup.manager.ConfigManager;
import ru.cmegma.cmcatchingup.manager.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetArenaCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MessagesManager messagesManager;

    public SetArenaCommand(Cmcatchingup plugin) {
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

        configManager.setArenaLocation(player.getLocation());
        player.sendMessage(messagesManager.getSimpleMessage("command.setarena.success"));
        return true;
    }
}