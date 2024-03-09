package me.lamalditag.hungergamesulp.handler;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.lamalditag.hungergamesulp.HungerGamesULP;

public class WorldBorderHandler implements Listener {
    
    private final JavaPlugin plugin;
    private FileConfiguration arenaConfig = null;
    private File arenaFile = null;

    public WorldBorderHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        createArenaConfig();
    }

    public void createArenaConfig() {
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
    }

    public FileConfiguration getArenaConfig() {
        if (arenaConfig == null) {
            createArenaConfig();
        }
        return arenaConfig;
    }

    public void initializeBorder() {
        World world = plugin.getServer().getWorlds().get(0);
        String worldName = world.getName();
        if (!arenaConfig.contains("arenas." + worldName + ".border")) {
            return;
        }

        WorldBorder border = world.getWorldBorder();

        double centerX = arenaConfig.getDouble("arenas." + worldName + ".border.center-x");
        double centerZ = arenaConfig.getDouble("arenas." + worldName + ".border.center-z");
        border.setCenter(centerX, centerZ);

        double borderSize = arenaConfig.getDouble("arenas." + worldName + ".border.size");
        border.setSize(borderSize);
    }

    public void startBorderShrink() {
        World world = plugin.getServer().getWorlds().get(0);
        String worldName = world.getName();
        if (!arenaConfig.contains("arenas." + worldName + ".border")) {
            return;
        }

        long startTime = arenaConfig.getLong("arenas." + worldName + ".border.start-time");
        long endTime = arenaConfig.getLong("arenas." + worldName + ".border.end-time");
        double finalSize = arenaConfig.getDouble("arenas." + worldName + ".border.final-size");

        WorldBorder border = world.getWorldBorder();

        double centerX = arenaConfig.getDouble("arenas." + worldName + ".border.center-x");
        double centerZ = arenaConfig.getDouble("arenas." + worldName + ".border.center-z");
        border.setCenter(centerX, centerZ);

        long duration = endTime - startTime;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (((HungerGamesULP) plugin).gameStarted) {
                border.setSize(finalSize, duration);
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(ChatColor.GOLD + "The world border has started to shrink!");
                }
            } else {
                double borderSize = arenaConfig.getDouble("arenas." + worldName + ".border.size");
                border.setSize(borderSize);
            }
        }, startTime * 20);
    }
}
