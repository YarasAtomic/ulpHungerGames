package me.lamalditag.hungergamesulp.commands;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import me.lamalditag.hungergamesulp.utils.ChestFill;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChestRefillCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    Map<String, Color> colorMap = new HashMap<>();
    private FileConfiguration chestLocations = null;
    private File chestFile = null;

    public ChestRefillCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        colorMap.put("AQUA", Color.AQUA);
        colorMap.put("BLACK", Color.BLACK);
        colorMap.put("BLUE", Color.BLUE);
        colorMap.put("FUCHSIA", Color.FUCHSIA);
        colorMap.put("GRAY", Color.GRAY);
        colorMap.put("GREEN", Color.GREEN);
        colorMap.put("LIME", Color.LIME);
        colorMap.put("MAROON", Color.MAROON);
        colorMap.put("NAVY", Color.NAVY);
        colorMap.put("OLIVE", Color.OLIVE);
        colorMap.put("ORANGE", Color.ORANGE);
        colorMap.put("PURPLE", Color.PURPLE);
        colorMap.put("RED", Color.RED);
        colorMap.put("SILVER", Color.SILVER);
        colorMap.put("TEAL", Color.TEAL);
        colorMap.put("WHITE", Color.WHITE);
        colorMap.put("YELLOW", Color.YELLOW);
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

    public FileConfiguration getArenaConfig() {
        File arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }
        return YamlConfiguration.loadConfiguration(arenaFile);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chestrefill")) {
            FileConfiguration ArenaConfig = getArenaConfig();
            FileConfiguration config = plugin.getConfig();
            World world = plugin.getServer().getWorlds().get(0);
            String worldName = world.getName();
            if (!getArenaConfig().contains("arenas." + worldName)) {
                sender.sendMessage(ChatColor.RED + "Create an arena first to run this command!");
                return true;
            }

            double pos1x = ArenaConfig.getDouble("arenas." + worldName + ".region.pos1.x");
            double pos1y = ArenaConfig.getDouble("arenas." + worldName + ".region.pos1.y");
            double pos1z = ArenaConfig.getDouble("arenas." + worldName + ".region.pos1.z");
            double pos2x = ArenaConfig.getDouble("arenas." + worldName + ".region.pos2.x");
            double pos2y = ArenaConfig.getDouble("arenas." + worldName + ".region.pos2.y");
            double pos2z = ArenaConfig.getDouble("arenas." + worldName + ".region.pos2.z");

            int minX = (int) Math.min(pos1x, pos2x);
            int minY = (int) Math.min(pos1y, pos2y);
            int minZ = (int) Math.min(pos1z, pos2z);
            int maxX = (int) Math.max(pos1x, pos2x);
            int maxY = (int) Math.max(pos1y, pos2y);
            int maxZ = (int) Math.max(pos1z, pos2z);

            FileConfiguration itemsConfig;
            File itemsFile = new File(plugin.getDataFolder(), "items.yml");
            if (itemsFile.exists()) {
                itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            } else {
                try {
                    itemsConfig = new YamlConfiguration();
                    itemsConfig.set("chest-items", new ArrayList<>());
                    itemsConfig.save(itemsFile);
                    sender.sendMessage(ChatColor.YELLOW + "Created new items.yml file!");
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Could not create items.yml file!");
                    return true;
                }
            }

            List<ItemStack> chestItems = new ArrayList<>();
            List<Integer> chestItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("chest-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    item = ChestFill.potion(item, itemMap);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    item = ChestFill.firework(item, itemMap, colorMap);
                } else if (itemMap.containsKey("enchantments")) {
                    item = ChestFill.enchantedItem(item, itemMap);
                } 
                item = ChestFill.item(item, itemMap);
                chestItems.add(item);
                chestItemWeights.add(weight);

            }

            List<ItemStack> barrelItems = new ArrayList<>();
            List<Integer> barrelItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("barrel-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    item = ChestFill.potion(item, itemMap);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    item = ChestFill.firework(item, itemMap, colorMap);
                } else if (itemMap.containsKey("enchantments")) {
                    item = ChestFill.enchantedItem(item, itemMap);
                }
                item = ChestFill.item(item, itemMap);
                barrelItems.add(item);
                barrelItemWeights.add(weight);
            }

            List<ItemStack> trappedChestItems = new ArrayList<>();
            List<Integer> trappedChestItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("trapped-chest-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    item = ChestFill.potion(item, itemMap);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    item = ChestFill.firework(item, itemMap, colorMap);
                } else if (itemMap.containsKey("enchantments")) {
                    item = ChestFill.enchantedItem(item, itemMap);
                }
                item = ChestFill.item(item, itemMap);
                trappedChestItems.add(item);
                trappedChestItemWeights.add(weight);
            }

            if (!getChestLocations().contains("locations." + worldName) || !getChestLocations().contains("bonus-locations." + worldName) || !getChestLocations().contains("mid-locations." + worldName)) {
                sender.sendMessage(ChatColor.RED + "Scan this arena first!");
                return true;
            }

            List<Location> chestLocations = getChestLocations().getList("locations." + worldName).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();
            List<Location> barrelLocations = getChestLocations().getList("bonus-locations." + worldName).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();
            List<Location> trappedChestLocations = getChestLocations().getList("mid-locations." + worldName).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(Location::deserialize)
                    .toList();

            for (Location location : chestLocations) {
                Block block = location.getBlock();
                if (block.getType() == Material.CHEST) {
                    Chest chest = (Chest) block.getState();
                    chest.getInventory().clear();

                    int minChestContent = config.getInt("min-chest-content");
                    int maxChestContent = config.getInt("max-chest-content");

                    Random rand = new Random();
                    int numItems = rand.nextInt(maxChestContent - minChestContent + 1) + minChestContent;
                    numItems = Math.min(numItems, chestItems.size());

                    List<ItemStack> randomItems = new ArrayList<>();
                    for (int i = 0; i < numItems; i++) {
                        int index = getRandomWeightedIndex(chestItemWeights);
                        randomItems.add(chestItems.get(index));
                    }

                    Set<Integer> usedSlots = new HashSet<>();
                    for (ItemStack item : randomItems) {
                        int slot = rand.nextInt(chest.getInventory().getSize());
                        while (usedSlots.contains(slot)) {
                            slot = rand.nextInt(chest.getInventory().getSize());
                        }
                        usedSlots.add(slot);
                        chest.getInventory().setItem(slot, item);
                    }
                }
            }

            for (Location location : barrelLocations) {
                Block block = location.getBlock();
                if (block.getType() == Material.BARREL) {
                    Barrel barrel = (Barrel) block.getState();
                    barrel.getInventory().clear();

                    int minBarrelContent = config.getInt("min-barrel-content");
                    int maxBarrelContent = config.getInt("max-barrel-content");

                    Random rand = new Random();
                    int numItems = rand.nextInt(maxBarrelContent - minBarrelContent + 1) + minBarrelContent;
                    numItems = Math.min(numItems, barrelItems.size());

                    List<ItemStack> randomItems = new ArrayList<>();
                    for (int i = 0; i < numItems; i++) {
                        int index = getRandomWeightedIndex(barrelItemWeights);
                        randomItems.add(barrelItems.get(index));
                    }

                    Set<Integer> usedSlots = new HashSet<>();
                    for (ItemStack item : randomItems) {
                        int slot = rand.nextInt(barrel.getInventory().getSize());
                        while (usedSlots.contains(slot)) {
                            slot = rand.nextInt(barrel.getInventory().getSize());
                        }
                        usedSlots.add(slot);
                        barrel.getInventory().setItem(slot, item);
                    }
                }
            }

            for (Location location : trappedChestLocations) {
                Block block = location.getBlock();
                if (block.getType() == Material.TRAPPED_CHEST) {
                    Inventory trappedChest = getItemStacks(block);

                    int mintrappedChestContent = config.getInt("min-trapped-chest-content");
                    int maxtrappedChestContent = config.getInt("max-trapped-chest-content");
                    Random rand = new Random();
                    int numItems = rand.nextInt(maxtrappedChestContent - mintrappedChestContent + 1) + mintrappedChestContent;
                    numItems = Math.min(numItems, trappedChestItems.size());

                    List<ItemStack> randomItems = new ArrayList<>();
                    for (int i = 0; i < numItems; i++) {
                        int index = getRandomWeightedIndex(trappedChestItemWeights);
                        randomItems.add(trappedChestItems.get(index));
                    }

                    Set<Integer> usedSlots = new HashSet<>();
                    for (ItemStack item : randomItems) {
                        int slot = rand.nextInt(trappedChest.getSize());
                        while (usedSlots.contains(slot)) {
                            slot = rand.nextInt(trappedChest.getSize());
                        }
                        usedSlots.add(slot);
                        trappedChest.setItem(slot, item);
                    }
                }
            }
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Chests have been refilled!");
        }
        return true;
    }

    @NotNull
    private static Inventory getItemStacks(Block block) {
        Inventory bonusChest;
        if (block.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) block.getState();
            bonusChest = barrel.getInventory();
        } else if (block.getType() == Material.TRAPPED_CHEST) {
            Chest chest = (Chest) block.getState();
            bonusChest = chest.getInventory();
        } else {
            ShulkerBox shulkerBox = (ShulkerBox) block.getState();
            bonusChest = shulkerBox.getInventory();
        }
        bonusChest.clear();
        return bonusChest;
    }

    private int getRandomWeightedIndex(List<Integer> weights) {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        int randInt = new Random().nextInt(totalWeight);
        for (int i = 0; i < weights.size(); i++) {
            randInt -= weights.get(i);
            if (randInt < 0) {
                return i;
            }
        }
        return -1;
    }
}



