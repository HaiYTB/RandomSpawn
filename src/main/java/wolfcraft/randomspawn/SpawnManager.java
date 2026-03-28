package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {
    private final RandomSpawn plugin;
    private FileConfiguration config;
    private final Random random;

    private final ConcurrentHashMap<String, Set<Location>> safeLocationsCache;

    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private int zMin;
    private int zMax;
    private boolean forceGroundSpawn;
    private boolean enableFirstJoinSpawn;
    private boolean enableRespawnOnDeath;
    private int maxTries;
    private String transferServerName;
    private Set<String> enabledWorlds;
    private Set<String> fatalBlocks;

    public SpawnManager(RandomSpawn plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.safeLocationsCache = new ConcurrentHashMap<>();
        reloadConfig();
    }

    public void reloadConfig() {
        config = plugin.getConfig();

        xMin = config.getInt("spawn.x.min", -1000);
        xMax = config.getInt("spawn.x.max", 1000);
        yMin = config.getInt("spawn.y.min", 64);
        yMax = config.getInt("spawn.y.max", 128);
        zMin = config.getInt("spawn.z.min", -1000);
        zMax = config.getInt("spawn.z.max", 1000);
        forceGroundSpawn = config.getBoolean("spawn.force-ground-spawn", true);
        enableFirstJoinSpawn = config.getBoolean("events.first-join", true);
        enableRespawnOnDeath = config.getBoolean("events.respawn-on-death", true);
        transferServerName = config.getString("spawn.transfer-to-server", "");
        maxTries = config.getInt("spawn.max-tries", 50);
        fatalBlocks = new HashSet<>();
        for (String block : config.getStringList("fatal-blocks")) {
            fatalBlocks.add(block.toUpperCase());
        }

        enabledWorlds = new HashSet<>(config.getStringList("enabled-worlds"));
        if (enabledWorlds.isEmpty() && plugin.getServer().getWorlds().size() > 0) {
            enabledWorlds.add(plugin.getServer().getWorlds().get(0).getName());
        }

        safeLocationsCache.clear();
    }

    public boolean isFirstJoinEnabled() {
        return enableFirstJoinSpawn;
    }

    public boolean isRespawnOnDeathEnabled() {
        return enableRespawnOnDeath;
    }

    public boolean isWorldEnabled(String worldName) {
        return enabledWorlds.contains(worldName);
    }

    public String getTransferServerName() {
        return transferServerName;
    }

    public Location getRandomSpawnLocation(Player player) {
        World world = player.getWorld();

        if (!isWorldEnabled(world.getName())) {
            return null;
        }

        for (int attempt = 0; attempt < maxTries; attempt++) {
            Location location = generateRandomLocation(world);

            if (forceGroundSpawn) {
                location = findSafeYPosition(location);

                if (location != null) {
                    location = centerOnBlock(location);
                    location.setYaw(random.nextFloat() * 360);
                    location.setPitch(0);
                    cacheLocation(world.getName(), location);
                    return location;
                }
            } else if (isSafeLocation(location)) {
                location = centerOnBlock(location);
                location.setYaw(random.nextFloat() * 360);
                location.setPitch(0);
                cacheLocation(world.getName(), location);
                return location;
            }
        }

        Set<Location> cachedLocations = safeLocationsCache.get(world.getName());
        if (cachedLocations != null && !cachedLocations.isEmpty()) {
            List<Location> validCached = new ArrayList<>();
            for (Location loc : cachedLocations) {
                if (isStillSafe(loc)) {
                    validCached.add(loc);
                }
            }

            cachedLocations.removeIf(loc -> !validCached.contains(loc));

            if (!validCached.isEmpty()) {
                return validCached.get(random.nextInt(validCached.size()));
            }
        }

        return null;
    }

    private Location generateRandomLocation(World world) {
        int x = randomBetween(xMin, xMax);
        int z = randomBetween(zMin, zMax);

        if (forceGroundSpawn) {
            return new Location(world, x, world.getMaxHeight() - 1, z);
        } else {
            int y = randomBetween(yMin, yMax);
            return new Location(world, x, y, z);
        }
    }

    private Location findSafeYPosition(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

            if (!block.getType().isAir() &&
                blockAbove.getType().isAir() &&
                blockTwoAbove.getType().isAir() &&
                !block.isLiquid() &&
                !isFatalBlock(block.getType().toString())) {

                return new Location(world, x, y + 1, z);
            }
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (forceGroundSpawn) {
            return true;
        }

        Block block = location.getBlock();
        Block blockBelow = location.clone().add(0, -1, 0).getBlock();
        Block blockAbove = location.clone().add(0, 1, 0).getBlock();

        return block.getType().isAir() &&
           blockAbove.getType().isAir() &&
           !blockBelow.getType().isAir() &&
           !blockBelow.isLiquid() &&
           !isFatalBlock(blockBelow.getType().toString());
    }

    private boolean isStillSafe(Location location) {
        if (location == null || location.getWorld() == null) return false;

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        return feet.getType().isAir() &&
            head.getType().isAir() &&
            !ground.getType().isAir() &&
            !ground.isLiquid() &&
            !isFatalBlock(ground.getType().toString());
    }

    private boolean isFatalBlock(String blockType) {
        return fatalBlocks.contains(blockType.toUpperCase());
    }

    private Location centerOnBlock(Location location) {
        location.setX(location.getBlockX() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);
        return location;
    }

    private void cacheLocation(String worldName, Location location) {
        Set<Location> locations = safeLocationsCache.computeIfAbsent(
            worldName, k -> ConcurrentHashMap.newKeySet()
        );

        locations.add(location.clone());

        if (locations.size() > 50) {
            Location[] locArray = locations.toArray(new Location[0]);
            locations.remove(locArray[random.nextInt(locArray.length)]);
        }
    }

    private int randomBetween(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
