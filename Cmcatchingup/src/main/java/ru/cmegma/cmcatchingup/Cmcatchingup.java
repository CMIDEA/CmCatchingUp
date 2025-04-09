package ru.cmegma.cmcatchingup;

import ru.cmegma.cmcatchingup.commands.PlayCommand;
import ru.cmegma.cmcatchingup.commands.SetArenaCommand;
import ru.cmegma.cmcatchingup.commands.SetLobbyCommand;
import ru.cmegma.cmcatchingup.listeners.GameListener;
import ru.cmegma.cmcatchingup.manager.ConfigManager;
import ru.cmegma.cmcatchingup.manager.GameManager;
import ru.cmegma.cmcatchingup.manager.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

public final class Cmcatchingup extends JavaPlugin {

    private GameManager gameManager;
    private ConfigManager configManager;
    private MessagesManager messagesManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages();

        gameManager = new GameManager(this);

        Objects.requireNonNull(getCommand("play")).setExecutor(new PlayCommand(this));
        Objects.requireNonNull(getCommand("setlobby")).setExecutor(new SetLobbyCommand(this));
        Objects.requireNonNull(getCommand("setarena")).setExecutor(new SetArenaCommand(this));

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        getLogger().info("CmCatchingUp zxc plug on!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && (gameManager.getGameState() == GameManager.GameState.RUNNING || gameManager.getGameState() == GameManager.GameState.COUNTDOWN)) {
            gameManager.forceEndGame();
        }
        cleanupScoreboardTeam();
        getLogger().info("CmCatchingUp zxc plug off!");
    }

    private void cleanupScoreboardTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team itTeam = board.getTeam(GameManager.IT_TEAM_NAME);
        if (itTeam != null) {
            try {
                itTeam.unregister();
            } catch (IllegalStateException e) {
                getLogger().warning("Failed to unregister team " + GameManager.IT_TEAM_NAME + ": " + e.getMessage());
            }
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}