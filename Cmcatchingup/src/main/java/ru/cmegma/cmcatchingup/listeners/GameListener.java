package ru.cmegma.cmcatchingup.listeners;

import ru.cmegma.cmcatchingup.manager.GameManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (gameManager.getGameState() != GameManager.GameState.RUNNING) {
            return;
        }

        Entity damagerEntity = event.getDamager();
        Entity damagedEntity = event.getEntity();

        if (damagerEntity instanceof Player damager && damagedEntity instanceof Player damaged) {
            if (gameManager.isPlayerInGame(damager) && gameManager.isPlayerInGame(damaged)) {
                event.setCancelled(true);
                if (gameManager.isItPlayer(damager)) {
                    gameManager.handleTag(damager, damaged);
                }
            }
        }
    }
}