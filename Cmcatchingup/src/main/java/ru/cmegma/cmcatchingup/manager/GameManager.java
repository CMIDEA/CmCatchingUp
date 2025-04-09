package ru.cmegma.cmcatchingup.manager;

import ru.cmegma.cmcatchingup.Cmcatchingup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    public enum GameState { WAITING, COUNTDOWN, RUNNING, ENDING }
    public static final String IT_TEAM_NAME = "CatchupItTeam";

    private final Cmcatchingup plugin;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private GameState gameState = GameState.WAITING;
    private final Set<UUID> playersInLobby = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Location> playersInGame = new ConcurrentHashMap<>();
    private UUID itPlayer = null;
    private BukkitTask countdownTask = null;
    private BukkitTask gameTimerTask = null;

    public GameManager(Cmcatchingup plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    public GameState getGameState() {
        return gameState;
    }

    public Set<UUID> getPlayersInGame() {
        return playersInGame.keySet();
    }

    public boolean isPlayerInGame(Player player) {
        return playersInGame.containsKey(player.getUniqueId());
    }

    public boolean isItPlayer(Player player) {
        return player.getUniqueId().equals(itPlayer);
    }

    public void addPlayerToLobby(Player player) {
        if (gameState != GameState.WAITING && gameState != GameState.COUNTDOWN) {
            player.sendMessage(messagesManager.getSimpleMessage("error.game-already-running"));
            return;
        }
        if (configManager.getLobbyLocation() == null) {
            player.sendMessage(messagesManager.getSimpleMessage("error.lobby-not-set"));
            return;
        }
        if (playersInLobby.contains(player.getUniqueId()) || playersInGame.containsKey(player.getUniqueId())) {
            player.sendMessage(messagesManager.getSimpleMessage("error.already-in-game"));
            return;
        }

        playersInLobby.add(player.getUniqueId());
        player.teleportAsync(configManager.getLobbyLocation());
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(messagesManager.getSimpleMessage("lobby.join"));
        broadcastToLobby(messagesManager.getMessage("lobby.player-join", Map.of(
                "player", player.getName(),
                "current", String.valueOf(playersInLobby.size()),
                "required", String.valueOf(configManager.getMinPlayers())
        )));

        checkStartCondition();
    }

    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        boolean removedFromLobby = playersInLobby.remove(playerId);
        Location originalLocation = playersInGame.remove(playerId);

        removePlayerFromTeam(player);
        removeGlowPotionEffect(player);

        if (originalLocation != null) {
            player.teleportAsync(originalLocation);
            player.setGameMode(Bukkit.getDefaultGameMode());
            broadcastToGame(messagesManager.getMessage("game.player-leave", Map.of("player", player.getName())));
            if (playerId.equals(itPlayer)) {
                itPlayer = null;
                if (gameState == GameState.RUNNING && playersInGame.size() > 1) {
                    chooseNewIt(null);
                } else if (gameState == GameState.RUNNING && playersInGame.size() <= 1) {
                    endGame(messagesManager.getRawMessage("game.end-not-enough-players", "Not enough players to continue."));
                }
            }
        } else if (removedFromLobby) {
            broadcastToLobby(messagesManager.getMessage("lobby.player-leave", Map.of("player", player.getName())));
            if (gameState == GameState.COUNTDOWN && playersInLobby.size() < configManager.getMinPlayers()) {
                cancelCountdown(messagesManager.getRawMessage("lobby.not-enough-players", "Not enough players."));
            }
        }
    }

    private void checkStartCondition() {
        if (gameState == GameState.WAITING && playersInLobby.size() >= configManager.getMinPlayers()) {
            startCountdown();
        }
    }

    private void startCountdown() {
        gameState = GameState.COUNTDOWN;
        broadcastToLobby(messagesManager.getMessage("lobby.countdown-start", Map.of(
                "seconds", String.valueOf(configManager.getCountdownSeconds())
        )));

        final int[] timer = {configManager.getCountdownSeconds()};

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer[0] > 0) {
                    Title title = Title.title(
                            messagesManager.getRawComponentMessage("title.countdown.title", Map.of("seconds", String.valueOf(timer[0]))),
                            messagesManager.getRawComponentMessage("title.countdown.subtitle", null),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                    );
                    sendTitleToLobby(title);
                    timer[0]--;
                } else {
                    this.cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void cancelCountdown(String reason) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        gameState = GameState.WAITING;
        Title title = Title.title(
                messagesManager.getRawComponentMessage("title.countdown-cancelled.title", null),
                messagesManager.getRawComponentMessage("title.countdown-cancelled.subtitle", Map.of("reason", reason)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
        );
        sendTitleToLobby(title);
        broadcastToLobby(messagesManager.getMessage("lobby.countdown-cancel", Map.of("reason", reason)));
    }


    private void startGame() {
        if (configManager.getArenaLocation() == null) {
            broadcastToLobby(messagesManager.getSimpleMessage("error.arena-not-set"));
            forceEndGame();
            return;
        }

        gameState = GameState.RUNNING;

        for (UUID playerId : new HashSet<>(playersInLobby)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                playersInGame.put(playerId, player.getLocation());
                player.teleportAsync(configManager.getArenaLocation());
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                removePlayerFromTeam(player);
            } else {
                playersInLobby.remove(playerId);
            }
        }
        playersInLobby.clear();


        if (playersInGame.size() < 2) {
            broadcastToGame(messagesManager.getSimpleMessage("game.start-failed-players"));
            forceEndGame();
            return;
        }

        broadcastToGame(messagesManager.getSimpleMessage("game.start"));
        chooseNewIt(null);
        startGameTimer();
    }

    private void startGameTimer() {
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                endGame(messagesManager.getRawMessage("game.end-timer", "Time is up!"));
            }
        }.runTaskLater(plugin, configManager.getGameDurationSeconds() * 20L);
    }

    private void endGame(String reason) {
        if (gameState != GameState.RUNNING && gameState != GameState.COUNTDOWN) return;

        gameState = GameState.ENDING;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        broadcastToGame(messagesManager.getSimpleMessage("game.end-title")
                .append(Component.text(" "))
                .append(messagesManager.getMessage("game.end-reason", Map.of("reason", reason))));

        itPlayer = null;

        Map<UUID, Location> playersToTeleport = new HashMap<>(playersInGame);
        playersInGame.clear();

        for (Map.Entry<UUID, Location> entry : playersToTeleport.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                removePlayerFromTeam(player);
                removeGlowPotionEffect(player);
                player.teleportAsync(entry.getValue());
                player.setGameMode(Bukkit.getDefaultGameMode());
            }
        }

        gameState = GameState.WAITING;
    }

    public void forceEndGame() {
        if (gameState == GameState.WAITING) return;

        boolean wasRunning = gameState == GameState.RUNNING;
        gameState = GameState.ENDING;

        if (gameTimerTask != null) gameTimerTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        gameTimerTask = null;
        countdownTask = null;

        Component endMessage = messagesManager.getSimpleMessage("game.force-end");

        itPlayer = null;

        Map<UUID, Location> playersToTeleport = new HashMap<>(playersInGame);
        playersInGame.clear();

        Set<UUID> lobbyPlayersToReset = new HashSet<>(playersInLobby);
        playersInLobby.clear();

        for (Map.Entry<UUID, Location> entry : playersToTeleport.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                if (wasRunning) player.sendMessage(endMessage);
                removePlayerFromTeam(player);
                removeGlowPotionEffect(player);
                player.teleportAsync(entry.getValue());
                player.setGameMode(Bukkit.getDefaultGameMode());
            }
        }

        for (UUID lobbyPlayerId : lobbyPlayersToReset) {
            Player player = Bukkit.getPlayer(lobbyPlayerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(endMessage);
                removePlayerFromTeam(player);
                removeGlowPotionEffect(player);
                if (player.getGameMode() == GameMode.ADVENTURE && configManager.getLobbyLocation() != null && player.getWorld().equals(configManager.getLobbyLocation().getWorld())) {
                    player.setGameMode(Bukkit.getDefaultGameMode());
                }
            }
        }
        gameState = GameState.WAITING;
    }


    public void handleTag(Player tagger, Player tagged) {
        if (gameState != GameState.RUNNING || !tagger.getUniqueId().equals(itPlayer) || !playersInGame.containsKey(tagged.getUniqueId())) {
            return;
        }
        if (tagger.getUniqueId().equals(tagged.getUniqueId())) return;

        broadcastToGame(messagesManager.getMessage("game.tag", Map.of(
                "tagged", tagged.getName(),
                "tagger", tagger.getName()
        )));

        setNewIt(tagged, tagger);
    }

    private void chooseNewIt(UUID playerToExclude) {
        List<UUID> possiblePlayers = new ArrayList<>(playersInGame.keySet());
        if (playerToExclude != null) {
            possiblePlayers.remove(playerToExclude);
        }

        if (possiblePlayers.isEmpty()) {
            if (!playersInGame.isEmpty()) {
                UUID remaining = playersInGame.keySet().iterator().next();
                if (!remaining.equals(playerToExclude)) {
                    Player newItPlayer = Bukkit.getPlayer(remaining);
                    if (newItPlayer != null) {
                        setNewIt(newItPlayer, null);
                        broadcastToGame(messagesManager.getMessage("game.new-it-alone", Map.of("player", newItPlayer.getName())));
                        return;
                    }
                }
            }
            endGame(messagesManager.getRawMessage("game.no-one-to-tag", "No one left to tag."));
            return;
        }

        UUID newItId = possiblePlayers.get(new Random().nextInt(possiblePlayers.size()));
        Player newItPlayer = Bukkit.getPlayer(newItId);

        if (newItPlayer != null) {
            Player oldItPlayer = playerToExclude != null ? Bukkit.getPlayer(playerToExclude) : null;
            setNewIt(newItPlayer, oldItPlayer);
            if(oldItPlayer == null) {
                broadcastToGame(messagesManager.getMessage("game.new-it-first", Map.of("player", newItPlayer.getName())));
            }
        } else {
            playersInGame.remove(newItId);
            chooseNewIt(playerToExclude);
        }
    }

    private void setNewIt(Player newIt, Player oldIt) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team itTeam = board.getTeam(IT_TEAM_NAME);
        if (itTeam == null) {
            itTeam = board.registerNewTeam(IT_TEAM_NAME);
        }
        itTeam.color(configManager.getGlowColor());
        itTeam.setAllowFriendlyFire(false);
        itTeam.setCanSeeFriendlyInvisibles(true);

        if (oldIt != null) {
            itTeam.removeEntry(oldIt.getName());
            removeGlowPotionEffect(oldIt);
        }
        if (newIt != null) {
            this.itPlayer = newIt.getUniqueId();
            itTeam.addEntry(newIt.getName());
            applyGlowPotionEffect(newIt);
        } else {
            this.itPlayer = null;
        }
    }

    private void applyGlowPotionEffect(Player player) {
        if (player != null && player.isOnline()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        }
    }

    private void removeGlowPotionEffect(Player player) {
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    private void removePlayerFromTeam(Player player) {
        if (player == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team itTeam = board.getTeam(IT_TEAM_NAME);
        if (itTeam != null) {
            itTeam.removeEntry(player.getName());
        }
    }

    private void broadcastToLobby(Component message) {
        for (UUID playerId : playersInLobby) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    private void sendTitleToLobby(Title title) {
        for (UUID playerId : playersInLobby) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.showTitle(title);
            }
        }
    }


    private void broadcastToGame(Component message) {
        for (UUID playerId : playersInGame.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}