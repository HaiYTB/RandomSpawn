package wolfcraft.randomspawn;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class RandomSpawn extends JavaPlugin {
    private FileConfiguration config;
    private SpawnManager spawnManager;

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
        if (!isManagedCommand(cmd.getName())) {
            return false;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if ("reload".equals(subCommand)) {
            if (!sender.hasPermission("randomspawn.reload")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }

            reloadPluginConfig();
            sender.sendMessage(getMessage("reload"));
            return true;
        }

        if ("status".equals(subCommand)) {
            sendStatus(sender);
            return true;
        }

        if ("cache".equals(subCommand) && args.length > 1 && "clear".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission("randomspawn.manage")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }

            spawnManager.clearCache();
            sender.sendMessage(getMessage("cache-cleared"));
            return true;
        }

        showHelp(sender);
        return true;
    }

    public String getMessage(String key) {
        String prefix = ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages.prefix", "&6[RandomSpawn] &r"));
        String msg = ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages." + key, ""));
        return prefix + msg;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== RandomSpawn Help ===");
        sender.sendMessage(ChatColor.GOLD + "/rd reload" + ChatColor.WHITE + " - Reload the configuration");
        sender.sendMessage(ChatColor.GOLD + "/rd status" + ChatColor.WHITE + " - Show current spawn settings");
        sender.sendMessage(ChatColor.GOLD + "/rd cache clear" + ChatColor.WHITE + " - Clear the safe spawn cache");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
        spawnManager.reloadConfig();
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== RandomSpawn Status ===");
        sender.sendMessage(ChatColor.GRAY + "Folia: " + ChatColor.WHITE + (FoliaUtils.isFolia() ? "enabled" : "disabled"));
        sender.sendMessage(ChatColor.GRAY + "First join spawn: " + ChatColor.WHITE + spawnManager.isFirstJoinEnabled());
        sender.sendMessage(ChatColor.GRAY + "Respawn spawn: " + ChatColor.WHITE + spawnManager.isRespawnOnDeathEnabled());
        sender.sendMessage(ChatColor.GRAY + "Join delay: " + ChatColor.WHITE + spawnManager.getJoinDelayTicks() + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Transfer delay: " + ChatColor.WHITE + spawnManager.getTransferDelayTicks() + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Cache limit: " + ChatColor.WHITE + spawnManager.getCacheLimit());
        sender.sendMessage(ChatColor.GRAY + "Cached locations: " + ChatColor.WHITE + spawnManager.getTotalCacheSize());
        sender.sendMessage(ChatColor.GRAY + "Enabled worlds: " + ChatColor.WHITE + String.join(", ", spawnManager.getEnabledWorlds()));
        String transferServer = spawnManager.getTransferServerName();
        sender.sendMessage(ChatColor.GRAY + "Transfer server: " + ChatColor.WHITE +
            (transferServer == null || transferServer.isEmpty() ? "none" : transferServer));
    }

    private boolean isManagedCommand(String commandName) {
        return "rd".equalsIgnoreCase(commandName)
            || "random".equalsIgnoreCase(commandName)
            || "randomspawn".equalsIgnoreCase(commandName);
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
