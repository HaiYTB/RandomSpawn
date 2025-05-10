package wolfcraft.randomspawn;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {
    private final RandomSpawn plugin;
    private FileConfiguration config;
    private final Random random;
    
    // Cache to store safe teleport locations
    private final ConcurrentHashMap<String, Set<Location>> safeLocationsCache;
    
    // Configuration settings
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
    private Set<String> enabledWorlds;
    
    public SpawnManager(RandomSpawn plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.safeLocationsCache = new ConcurrentHashMap<>();
        reloadConfig();
    }
    
    public void reloadConfig() {
        config = plugin.getConfig();
        
        // Load configuration values
        xMin = config.getInt("spawn.x.min", -1000);
        xMax = config.getInt("spawn.x.max", 1000);
        yMin = config.getInt("spawn.y.min", 64);
        yMax = config.getInt("spawn.y.max", 128);
        zMin = config.getInt("spawn.z.min", -1000);
        zMax = config.getInt("spawn.z.max", 1000);
        forceGroundSpawn = config.getBoolean("spawn.force-ground-spawn", true);
        enableFirstJoinSpawn = config.getBoolean("events.first-join", true);
        enableRespawnOnDeath = config.getBoolean("events.respawn-on-death", true);
        maxTries = config.getInt("spawn.max-tries", 50);
        
        // Load enabled worlds
        enabledWorlds = new HashSet<>(config.getStringList("enabled-worlds"));
        
        // If no worlds specified, add the default world
        if (enabledWorlds.isEmpty() && plugin.getServer().getWorlds().size() > 0) {
            enabledWorlds.add(plugin.getServer().getWorlds().get(0).getName());
        }
        
        // Clear cache when reloading config
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
    
    public Location getRandomSpawnLocation(Player player) {
        World world = player.getWorld();
        
        // Check if the world is enabled for random spawns
        if (!isWorldEnabled(world.getName())) {
            return null;
        }
        
        // Try to find a safe location to teleport to
        for (int attempt = 0; attempt < maxTries; attempt++) {
            Location location = generateRandomLocation(world);
            
            if (isSafeLocation(location)) {
                // Find the safe Y position if forceGroundSpawn is enabled
                if (forceGroundSpawn) {
                    location = findSafeYPosition(location);
                }
                
                // If we found a safe location, return it
                if (location != null) {
                    // Center the player on the block and face a random direction
                    location = centerOnBlock(location);
                    location.setYaw(random.nextFloat() * 360);
                    location.setPitch(0);
                    
                    // Cache this location for possible reuse
                    cacheLocation(world.getName(), location);
                    
                    return location;
                }
            }
        }
        
        // If we couldn't find a safe location after maxTries attempts,
        // try to use a cached location if available
        Set<Location> cachedLocations = safeLocationsCache.get(world.getName());
        if (cachedLocations != null && !cachedLocations.isEmpty()) {
            // Convert to array for random selection
            Location[] locations = cachedLocations.toArray(new Location[0]);
            return locations[random.nextInt(locations.length)];
        }
        
        // If all else fails, return null and let the server handle it
        return null;
    }
    
    private Location generateRandomLocation(World world) {
        int x = randomBetween(xMin, xMax);
        int z = randomBetween(zMin, zMax);
        
        if (forceGroundSpawn) {
            // If we're forcing ground spawn, start from a high Y and find ground
            return new Location(world, x, world.getMaxHeight() - 1, z);
        } else {
            // Otherwise, use the configured Y range
            int y = randomBetween(yMin, yMax);
            return new Location(world, x, y, z);
        }
    }
    
    private Location findSafeYPosition(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        // Start from the top and work our way down to find the highest non-air block
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
            
            // Check if this block is solid and the two blocks above it are air
            if (!block.getType().isAir() && 
                blockAbove.getType().isAir() && 
                blockTwoAbove.getType().isAir() &&
                !block.isLiquid() &&
                !isFatalBlock(block.getType().toString())) {
                
                // Return the location on top of this block
                return new Location(world, x, y + 1, z);
            }
        }
        
        // If we couldn't find a safe position, return null
        return null;
    }
    
    private boolean isSafeLocation(Location location) {
        // Check if the location is valid
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        Block block = location.getBlock();
        Block blockBelow = location.clone().add(0, -1, 0).getBlock();
        Block blockAbove = location.clone().add(0, 1, 0).getBlock();
        
        // If forceGroundSpawn is disabled, do a simple safety check
        if (!forceGroundSpawn) {
            // The block at the player's feet and head should be air
            // The block below should be solid
            return block.getType().isAir() && 
                   blockAbove.getType().isAir() &&
                   !blockBelow.getType().isAir() &&
                   !blockBelow.isLiquid() &&
                   !isFatalBlock(blockBelow.getType().toString());
        }
        
        // For forceGroundSpawn, the check is done in findSafeYPosition
        return true;
    }
    
    private boolean isFatalBlock(String blockType) {
        // List of blocks that would be fatal for players to spawn on
        String[] fatalBlocks = {
            "LAVA", "FIRE", "CACTUS", "MAGMA_BLOCK", "CAMPFIRE", 
            "SOUL_CAMPFIRE", "WITHER_ROSE", "SWEET_BERRY_BUSH"
        };
        
        blockType = blockType.toUpperCase();
        for (String fatalBlock : fatalBlocks) {
            if (blockType.contains(fatalBlock)) {
                return true;
            }
        }
        
        return false;
    }
    
    private Location centerOnBlock(Location location) {
        // Center the player on the block
        location.setX(location.getBlockX() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);
        return location;
    }
    
    private void cacheLocation(String worldName, Location location) {
        // Cache the location for possible reuse
        safeLocationsCache.computeIfAbsent(worldName, k -> new HashSet<>());
        
        Set<Location> locations = safeLocationsCache.get(worldName);
        locations.add(location.clone());
        
        // Limit the number of cached locations to prevent memory leaks
        if (locations.size() > 50) {
            // Convert to array for random removal
            Location[] locArray = locations.toArray(new Location[0]);
            locations.remove(locArray[random.nextInt(locArray.length)]);
        }
    }
    
    private int randomBetween(int min, int max) {
        // Ensure min <= max
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        
        // Use ThreadLocalRandom for better performance
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
