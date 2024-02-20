package me.lamalditag.hungergamesulp.commands;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import me.lamalditag.hungergamesulp.utils.ChestFill;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SupplyDropCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    Map<String, Color> colorMap = new HashMap<>();

    public SupplyDropCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getArenaConfig() {
        File arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }
        return YamlConfiguration.loadConfiguration(arenaFile);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("supplydrop")) {
            FileConfiguration config = plugin.getConfig();
            FileConfiguration arenaConfig = getArenaConfig();
            String worldName = arenaConfig.getString("region.world");
            if (worldName == null) {
                sender.sendMessage(ChatColor.RED + "Create an arena first to run this command!");
                return true;
            }
            World world = plugin.getServer().getWorld(worldName);

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

            List<ItemStack> supplyDropItems = new ArrayList<>();
            List<Integer> supplyDropItemWeights = new ArrayList<>();
            for (Map<?, ?> itemMap : itemsConfig.getMapList("supply-drop-items")) {
                String type = (String) itemMap.get("type");
                int weight = (int) itemMap.get("weight");
                int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
                ItemStack item = new ItemStack(Material.valueOf(type), amount);
                item = ChestFill.item(item, itemMap);
                if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
                    item = ChestFill.potion(item, itemMap);
                } else if (item.getType() == Material.FIREWORK_ROCKET) {
                    item = ChestFill.firework(item, itemMap, colorMap);
                } else if (itemMap.containsKey("enchantments")) {
                    item = ChestFill.enchantedItem(item, itemMap);
                }
                supplyDropItems.add(item);
                supplyDropItemWeights.add(weight);

            }

            int numSupplyDrops = config.getInt("num-supply-drops");
            int minSupplyDropContent = config.getInt("min-supply-drop-content");
            int maxSupplyDropContent = config.getInt("max-supply-drop-content");

            List<String> coords = new ArrayList<>();

            Random rand = new Random();
            for (int i = 0; i < numSupplyDrops; i++) {
                assert world != null;
                WorldBorder border = world.getWorldBorder();

                Location center = border.getCenter();
                double size = border.getSize();

                double minX = center.getX() - size / 2;
                double minZ = center.getZ() - size / 2;
                double maxX = center.getX() + size / 2;
                double maxZ = center.getZ() + size / 2;

                int x;
                int z;
                Block highestBlock;
                do {
                    x = (int) (rand.nextDouble() * (maxX - minX) + minX);
                    z = (int) (rand.nextDouble() * (maxZ - minZ) + minZ);
                    highestBlock = world.getHighestBlockAt(x, z);
                } while (highestBlock.getType() == Material.AIR || highestBlock.getY() < -60);

                if (highestBlock.getType() == Material.AIR) {
                    continue;
                }

                Block block = highestBlock.getRelative(0, 1, 0);
                block.setType(Material.RED_SHULKER_BOX);
                ShulkerBox shulkerBox = (ShulkerBox) block.getState();

                Location location = block.getLocation().add(0.5, 1, 0.5);
                Firework firework = (Firework) world.spawnEntity(location, EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();

                meta.setPower(3); // set the flight duration to 1
                FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL)
                        .withColor(Color.RED)
                        .withFade(Color.ORANGE)
                        .build();
                meta.addEffect(effect);
                firework.setFireworkMeta(meta);

                int numItems = rand.nextInt(maxSupplyDropContent - minSupplyDropContent + 1) + minSupplyDropContent;
                numItems = Math.min(numItems, supplyDropItems.size());

                List<ItemStack> randomItems = new ArrayList<>();
                for (int j = 0; j < numItems; j++) {
                    int index = getRandomWeightedIndex(supplyDropItemWeights);
                    randomItems.add(supplyDropItems.get(index));
                }

                coords.add("(" + x + ", " + highestBlock.getY() + ", " + z + ")");

                Set<Integer> usedSlots = new HashSet<>();
                for (ItemStack item : randomItems) {
                    int slot = rand.nextInt(shulkerBox.getInventory().getSize());
                    while (usedSlots.contains(slot)) {
                        slot = rand.nextInt(shulkerBox.getInventory().getSize());
                    }
                    usedSlots.add(slot);
                    shulkerBox.getInventory().setItem(slot, item);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.GREEN).append("Spawned ").append(numSupplyDrops).append(" supply drops at ");
            for (int i = 0; i < coords.size(); i++) {
                sb.append(coords.get(i));
                if (i < coords.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("!");

            plugin.getServer().broadcastMessage(sb.toString());
            return true;
        }
        return false;
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
