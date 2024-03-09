package me.lamalditag.hungergamesulp.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BorderSetCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private FileConfiguration arenaConfig = null;
    private File arenaFile = null;

    public BorderSetCommand(JavaPlugin plugin) {
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

    public void saveArenaConfig() {
        try {
            getArenaConfig().save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save arena.yml to " + arenaFile, e);
        }
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("border")) {
            if (args.length != 6) {
                sender.sendMessage("Usage: border <size> <center-x> <center-z> <final-size> <start-time> <end-time>");
                return true;
            }
            int newSize;
            double centerX, centerZ, finalSize;
            long startTime, endTime;
            try {
                newSize = Integer.parseInt(args[0]);
                centerX = Double.parseDouble(args[1]);
                centerZ = Double.parseDouble(args[2]);
                finalSize = Double.parseDouble(args[3]);
                startTime = Long.parseLong(args[4]);
                endTime = Long.parseLong(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid arguments. Please enter valid numbers.");
                return true;
            }
            World world = plugin.getServer().getWorlds().get(0);
            if (getArenaConfig().contains("arenas." + world.getName())) {
                WorldBorder border = world.getWorldBorder();
                border.setSize(newSize);
                border.setCenter(centerX, centerZ);
                sender.sendMessage("World border size set to " + newSize + " and center set to (" + centerX + ", " + centerZ + ")");
                getArenaConfig().set("arenas." + world.getName() + ".border.size", newSize);
                getArenaConfig().set("arenas." + world.getName() + ".border.center-x", centerX);
                getArenaConfig().set("arenas." + world.getName() + ".border.center-z", centerZ);
                getArenaConfig().set("arenas." + world.getName() + ".border.final-size", finalSize);
                getArenaConfig().set("arenas." + world.getName() + ".border.start-time", startTime);
                getArenaConfig().set("arenas." + world.getName() + ".border.end-time", endTime);
                saveArenaConfig();
            } else {
                sender.sendMessage(ChatColor.RED + "Arena not set. Please set the arena first.");
            }
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("border")) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                completions.add("<size>");
            } else if (args.length == 2) {
                completions.add("<center-x>");
            } else if (args.length == 3) {
                completions.add("<center-z>");
            } else if (args.length == 4) {
                completions.add("<final-size>");
            } else if (args.length == 5) {
                completions.add("<start-time>");
            } else if (args.length == 6) {
                completions.add("<end-time>");
            }
            return completions;
        }
        return null;
    }
}
