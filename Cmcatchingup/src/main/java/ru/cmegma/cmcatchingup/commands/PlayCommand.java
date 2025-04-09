package ru.cmegma.cmcatchingup.commands;

import ru.cmegma.cmcatchingup.Cmcatchingup;
import ru.cmegma.cmcatchingup.manager.GameManager;
import ru.cmegma.cmcatchingup.manager.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final MessagesManager messagesManager;

    public PlayCommand(Cmcatchingup plugin) {
        this.gameManager = plugin.getGameManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messagesManager.getSimpleMessage("error.players-only"));
            return true;
        }

        gameManager.addPlayerToLobby(player);
        return true;
    }
}