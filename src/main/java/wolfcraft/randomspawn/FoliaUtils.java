package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

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
                Method getScheduler = player.getClass().getMethod("getScheduler");
                Object scheduler = getScheduler.invoke(player);

                Method runDelayed = scheduler.getClass().getMethod(
                    "runDelayed", Plugin.class,
                    java.util.function.Consumer.class,
                    Runnable.class, long.class
                );
                runDelayed.invoke(scheduler, plugin,
                    (java.util.function.Consumer<Object>) t -> task.run(),
                    null, delayTicks
                );
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
                teleportAsync.invoke(player, location);
            } catch (Exception e) {
                plugin.getLogger().warning("FoliaUtils: teleportAsync failed - " + e.getMessage());
                player.teleport(location);
            }
        } else {
            player.teleport(location);
        }
    }
}