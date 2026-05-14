package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FoliaUtils {
    private static final boolean IS_FOLIA = checkFolia();

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runDelayed(Plugin plugin, Player player, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method runDelayed = scheduler.getClass().getMethod(
                    "runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class
                );
                runDelayed.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), null, delayTicks);
            } catch (Exception e) {
                plugin.getLogger().warning("FoliaUtils: runDelayed failed - " + e.getMessage());
                plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
            }
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void teleport(Plugin plugin, Player player, Location location) {
        if (IS_FOLIA) {
            try {
                Method teleportAsync = player.getClass().getMethod("teleportAsync", Location.class);
                Object future = teleportAsync.invoke(player, location);
                if (future instanceof CompletableFuture) {
                    ((CompletableFuture<Boolean>) future).whenComplete((success, ex) -> {
                        if (ex != null) {
                            plugin.getLogger().warning(
                                "FoliaUtils: teleportAsync failed - " + ex.getMessage()
                            );
                        } else if (Boolean.FALSE.equals(success)) {
                            plugin.getLogger().warning("FoliaUtils: teleportAsync returned false");
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("FoliaUtils: teleportAsync failed - " + e.getMessage());
                try {
                    Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                    Method run = scheduler.getClass().getMethod(
                        "run", Plugin.class, Consumer.class, Runnable.class
                    );
                    run.invoke(scheduler, plugin, (Consumer<Object>) ignored -> player.teleport(location), null);
                } catch (Exception fallbackException) {
                    plugin.getLogger().warning("FoliaUtils: entity scheduler fallback failed - " + fallbackException.getMessage());
                }
            }
        } else {
            player.teleport(location);
        }
    }
}
