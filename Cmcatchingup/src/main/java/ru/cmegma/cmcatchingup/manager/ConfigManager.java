package ru.cmegma.cmcatchingup.manager;

import ru.cmegma.cmcatchingup.Cmcatchingup;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class ConfigManager {

    private final Cmcatchingup plugin;
    private int minPlayers;
    private int countdownSeconds;
    private int gameDurationSeconds;
    private NamedTextColor glowColor;
    private Location lobbyLocation;
    private Location arenaLocation;

    public ConfigManager(Cmcatchingup plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        minPlayers = config.getInt("game.min-players", 2);
        countdownSeconds = config.getInt("game.countdown-seconds", 10);
        gameDurationSeconds = config.getInt("game.duration-seconds", 180);

        String colorName = config.getString("game.glow-color", "RED").toLowerCase();
        this.glowColor = NamedTextColor.NAMES.value(colorName);
        if (this.glowColor == null) {
            this.glowColor = NamedTextColor.RED;
            if (plugin.getMessagesManager() != null) {
                plugin.getLogger().warning(
                        plugin.getMessagesManager().getRawComponentMessage(
                                "error.invalid-glow-color",
                                Map.of("color", config.getString("game.glow-color","RED"))
                        ).toString()
                );
            } else {
                plugin.getLogger().warning("Invalid glow color '" + config.getString("game.glow-color") + "' in config.yml. Defaulting to RED.");
            }
        }

        if (config.contains("locations.lobby")) {
            lobbyLocation = getLocationFromConfig("locations.lobby");
        }
        if (config.contains("locations.arena")) {
            arenaLocation = getLocationFromConfig("locations.arena");
        }
    }

    private Location getLocationFromConfig(String path) {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            if (plugin.getMessagesManager() != null) {
                plugin.getLogger().warning(
                        plugin.getMessagesManager().getRawComponentMessage(
                                "error.world-not-found",
                                Map.of("world", worldName, "path", path)
                        ).toString()
                );
            } else {
                plugin.getLogger().warning("World '" + worldName + "' not found for location path: " + path);
            }
            return null;
        }
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocationToConfig(String path, Location location) {
        FileConfiguration config = plugin.getConfig();
        if (location == null || location.getWorld() == null) return;
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        saveLocationToConfig("locations.lobby", location);
    }

    public void setArenaLocation(Location location) {
        this.arenaLocation = location;
        saveLocationToConfig("locations.arena", location);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getGameDurationSeconds() {
        return gameDurationSeconds;
    }

    public NamedTextColor getGlowColor() {
        return glowColor;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public Location getArenaLocation() {
        return arenaLocation;
    }
}