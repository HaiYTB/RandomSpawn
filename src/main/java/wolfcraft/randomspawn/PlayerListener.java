package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

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

        // Only teleport on first join
        if (!player.hasPlayedBefore() && spawnManager.isFirstJoinEnabled()) {
            // Delay the teleport to ensure the player is fully loaded
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        teleportToRandomSpawn(player);
                    }
                }
            }.runTaskLater(plugin, 5L); // 5 ticks = 0.25 seconds
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

                    // If configured, transfer the player to another server after respawn
                    if (spawnManager.getTransferServerName() != null && !spawnManager.getTransferServerName().isEmpty()) {
                        // Delay the transfer to ensure the player is fully respawned
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    plugin.transferPlayerToServer(player, spawnManager.getTransferServerName());
                                }
                            }
                        }.runTaskLater(plugin, 5L); // 5 ticks = 0.25 seconds
                    }
                }
            }
        }
    }

    private void teleportToRandomSpawn(Player player) {
        Location randomLocation = spawnManager.getRandomSpawnLocation(player);

        if (randomLocation != null) {
            player.teleport(randomLocation);
        }
    }
}
