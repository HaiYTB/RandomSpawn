package wolfcraft.randomspawn;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class RandomSpawn extends JavaPlugin {
    private FileConfiguration config;
    private SpawnManager spawnManager;
    private final String PREFIX = ChatColor.GOLD + "[RandomSpawn] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        config = getConfig();

        // Initialize spawn manager
        spawnManager = new SpawnManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, spawnManager), this);

        // Register BungeeCord channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("RandomSpawn has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RandomSpawn has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Handle both command aliases: /rd and /random
        if (cmd.getName().equalsIgnoreCase("rd") || cmd.getName().equalsIgnoreCase("random")) {
            if (args.length == 0) {
                // Show help
                showHelp(sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                // Check permission
                if (!sender.hasPermission("randomspawn.reload")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }

                // Reload config
                reloadConfig();
                config = getConfig();
                spawnManager.reloadConfig();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuration reloaded successfully!");
                return true;
            }

            // Unknown argument, show help
            showHelp(sender);
            return true;
        }

        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== RandomSpawn Help ===");
        sender.sendMessage(ChatColor.GOLD + "/rd reload " + ChatColor.WHITE + "- Reload the configuration");
        sender.sendMessage(ChatColor.GOLD + "/random reload " + ChatColor.WHITE + "- Reload the configuration");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
        spawnManager.reloadConfig();
    }

    public void transferPlayerToServer(Player player, String serverName) {
        if (player == null || serverName == null || serverName.isEmpty()) return;

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (NoClassDefFoundError | Exception e) {
            getLogger().warning("Failed to send player to server '" + serverName + "': " + e.getMessage());
        }
    }
}
