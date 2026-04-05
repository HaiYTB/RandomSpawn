package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
    private final RandomSpawn plugin;
    private final SpawnManager spawnManager;

    public PlayerListener(RandomSpawn plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore() && spawnManager.isFirstJoinEnabled()) {
            FoliaUtils.runDelayed(plugin, player, () -> {
                if (player.isOnline()) teleportToRandomSpawn(player);
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Don't override if player has a bed or anchor respawn
        if (!event.isBedSpawn() && !event.isAnchorSpawn() && spawnManager.isRespawnOnDeathEnabled()) {
            Player player = event.getPlayer();

            // Only apply to worlds that are enabled
            if (spawnManager.isWorldEnabled(player.getWorld().getName())) {
                Location randomLocation = spawnManager.getRandomSpawnLocation(player);

                if (randomLocation != null) {
                    event.setRespawnLocation(randomLocation);

                    String transferServer = spawnManager.getTransferServerName();
                    if (transferServer != null && !transferServer.isEmpty()) {
                        FoliaUtils.runDelayed(plugin, player, () -> {
                            if (player.isOnline())
                                plugin.transferPlayerToServer(player, transferServer);
                        }, 5L);
                    }
                }
            }
        }
    }

    private void teleportToRandomSpawn(Player player) {
        Location randomLocation = spawnManager.getRandomSpawnLocation(player);

        if (randomLocation != null) {
            FoliaUtils.teleport(plugin, player, randomLocation);
        }
    }
}
