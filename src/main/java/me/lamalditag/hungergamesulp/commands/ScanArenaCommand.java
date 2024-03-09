package me.lamalditag.hungergamesulp.commands;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScanArenaCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private FileConfiguration arenaConfig = null;
    private File arenaFile = null;
    private FileConfiguration chestLocations = null;
    private File chestFile = null;

    public ScanArenaCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        createArenaConfig();
        createChestLocations();
    }

    public void createChestLocations() {
        chestFile = new File(plugin.getDataFolder(), "chest-locations.yml");
        if (!chestFile.exists()) {
            chestFile.getParentFile().mkdirs();
            plugin.saveResource("chest-locations.yml", false);
        }

        chestLocations = YamlConfiguration.loadConfiguration(chestFile);
    }

    public FileConfiguration getChestLocations() {
        if (chestLocations == null) {
            createChestLocations();
        }
        return chestLocations;
    }

    public void saveChestLocations() {
        try {
            getChestLocations().save(chestFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save chest-locations.yml to " + chestFile, e);
        }
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
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        return arenaConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("scanarena")) {
            // Run the scanning process asynchronously
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                FileConfiguration config = getArenaConfig();

                World world = plugin.getServer().getWorlds().get(0);
                String worldName = world.getName();
                if (!getArenaConfig().contains("arenas." + worldName + ".region")) {
                    sender.sendMessage(ChatColor.RED + "The arena region is not defined in the config!");
                    return;
                }

                sender.sendMessage(ChatColor.GREEN + "Scanning arena region... This may take a while.");

                double pos1x = config.getDouble("arenas." + worldName + ".region.pos1.x");
                double pos1y = config.getDouble("arenas." + worldName + ".region.pos1.y");
                double pos1z = config.getDouble("arenas." + worldName + ".region.pos1.z");
                double pos2x = config.getDouble("arenas." + worldName + ".region.pos2.x");
                double pos2y = config.getDouble("arenas." + worldName + ".region.pos2.y");
                double pos2z = config.getDouble("arenas." + worldName + ".region.pos2.z");

                int minX = (int) Math.min(pos1x, pos2x);
                int minY = (int) Math.min(pos1y, pos2y);
                int minZ = (int) Math.min(pos1z, pos2z);
                int maxX = (int) Math.max(pos1x, pos2x);
                int maxY = (int) Math.max(pos1y, pos2y);
                int maxZ = (int) Math.max(pos1z, pos2z);

                List<Location> chestLocations = new ArrayList<>();
                List<Location> barrelLocations = new ArrayList<>();
                List<Location> trappedChestLocations = new ArrayList<>();
                int minChunkX = minX >> 4;
                int maxChunkX = maxX >> 4;
                int minChunkZ = minZ >> 4;
                int maxChunkZ = maxZ >> 4;

                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        assert world != null;
                        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = minY; y <= maxY; y++) {
                                    Block block = chunk.getBlock(x, y, z);
                                    if (block.getType() == Material.CHEST) {
                                        chestLocations.add(block.getLocation());
                                    } else if (block.getType() == Material.BARREL) {
                                        barrelLocations.add(block.getLocation());
                                    } else if (block.getType() == Material.TRAPPED_CHEST) {
                                        trappedChestLocations.add(block.getLocation());
                                    }
                                }
                            }
                        }
                    }
                }

                getChestLocations().set("locations." + worldName, chestLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));

                getChestLocations().set("bonus-locations." + worldName, barrelLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));

                getChestLocations().set("mid-locations." + worldName, trappedChestLocations.stream()
                        .map(Location::serialize)
                        .collect(Collectors.toList()));

                saveChestLocations();
                sender.sendMessage(ChatColor.GREEN + "Chest locations have been saved!");
            });
            return true;
        }
        return false;
    }
}
