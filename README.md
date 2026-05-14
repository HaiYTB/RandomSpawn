<p align="center">
  <img src="https://raw.githubusercontent.com/HaiYTB/RandomSpawn/refs/heads/main/icon.png" width="256" alt="RandomSpawn Logo"/>
</p>

# RandomSpawn

RandomSpawn is a lightweight and high-performance Paper/Folia plugin that provides random spawn locations for players on first join and respawn after death.

## Features

- **Random Spawn on First Join**: New players will spawn at a random location within the configured area
- **Random Spawn on Death**: Players will respawn at a random location when they die
- **Folia Support**: Uses region-safe scheduling and async teleport when Folia is detected
- **Configurable Spawn Range**: 
  - X axis range (Min/Max)
  - Y axis range (Min/Max)
  - Z axis range (Min/Max)
- **Surface Spawn Option**: Option to force players to always spawn on the ground surface (prevents spawning in caves or air)
- **Per-World Configuration**: Enable random spawning only in specific worlds
- **Safe Spawn Protection**: Advanced algorithm ensures players only spawn in safe locations
- **High Performance**: Optimized code with configurable safe-location caching and low-overhead random selection
- **Built-in Utilities**: `/rd status` and `/rd cache clear` for faster diagnostics and maintenance

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rd` | Shows the help menu | None |
| `/rd reload` | Reloads the plugin configuration | `randomspawn.reload` |
| `/rd status` | Shows the current spawn configuration | None |
| `/rd cache clear` | Clears the safe spawn cache | `randomspawn.manage` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `randomspawn.reload` | Allows reloading the plugin configuration | OP |
| `randomspawn.manage` | Allows clearing the safe spawn cache | OP |

## Installation

1. Download the RandomSpawn.jar file
2. Place it in your server's `plugins` folder
3. Restart your server or use a plugin manager to load it
4. Edit the configuration file in `plugins/RandomSpawn/config.yml` as needed
5. Use `/rd reload` to apply changes

## Configuration

```yaml
# RandomSpawn Configuration

# Spawn location range
spawn:
  # X coordinate range
  x:
    min: -1000
    max: 1000
  
  # Y coordinate range (ignored if force-ground-spawn is true)
  y:
    min: 64
    max: 128
  
  # Z coordinate range
  z:
    min: -1000
    max: 1000
  
  # If true, players will always spawn on the surface
  # This will disable the Y coordinate range setting above
  force-ground-spawn: true

  # If set, players will be transferred to the specified server after spawning (supports BungeeCord/Waterfall/Velocity)
  transfer-to-server: ''
  
  # Maximum number of attempts to find a safe spawn location
  max-tries: 50

  # Maximum number of cached safe locations per world
  cache-size: 128

  # Delay before the first-join teleport runs
  join-delay-ticks: 5

  # Delay before the optional post-respawn transfer runs
  transfer-delay-ticks: 5

# List of dangerous blocks that players should not spawn on
fatal-blocks:
  - BEDROCK
  - LAVA
  - FIRE
  - CACTUS
  - MAGMA_BLOCK
  - CAMPFIRE
  - SOUL_CAMPFIRE
  - WITHER_ROSE
  - SWEET_BERRY_BUSH

# Event settings
events:
  # Enable random spawn on first join
  first-join: true
  
  # Enable random spawn on death
  respawn-on-death: true

# List of worlds where random spawn is enabled
# If empty, only the default world will be used
enabled-worlds:
  - world
  # - world_nether
  # - world_the_end

# Messages
messages:
  prefix: "&6[RandomSpawn] &r"
  reload: "&aConfiguration reloaded successfully!"
  cache-cleared: "&aSafe spawn cache cleared!"
  no-permission: "&cYou don't have permission to use this command!"
```

## Building from Source

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn clean package`
4. The compiled jar will be in the `target` folder

## Requirements

- Java 21
- Paper or Folia 1.20.1+

## Support

If you encounter any issues or have suggestions, please open an issue on the GitHub repository.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
