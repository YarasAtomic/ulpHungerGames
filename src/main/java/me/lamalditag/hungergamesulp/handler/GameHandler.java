package me.lamalditag.hungergamesulp.handler;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import me.lamalditag.hungergamesulp.HungerGamesULP;
import me.lamalditag.hungergamesulp.commands.ChestRefillCommand;
import me.lamalditag.hungergamesulp.commands.SupplyDropCommand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameHandler implements Listener {

    private final HungerGamesULP plugin;
    private final SetSpawnHandler setSpawnHandler;
    private FileConfiguration arenaConfig = null;
    private FileConfiguration scoreData = null;
    private File arenaFile = null;
    private File scoreFile = null;

    public GameHandler(HungerGamesULP plugin, SetSpawnHandler setSpawnHandler) {
        this.plugin = plugin;
        this.setSpawnHandler = setSpawnHandler;
        this.playersAlive = new ArrayList<>();
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

    public void createScoreData(){
        scoreFile = new File(plugin.getDataFolder(), "score.yml");
        if (!scoreFile.exists()) {
            scoreFile.getParentFile().mkdirs();
            plugin.saveResource("score.yml", false);
        }
        
        scoreData = YamlConfiguration.loadConfiguration(scoreFile);
    }

    public FileConfiguration getScoreData(){
        if (scoreData == null) {
            createScoreData();
        }
        return scoreData;
    }

    public void saveScoreData() {
        try {
            getScoreData().save(scoreFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save score.yml to " + scoreFile, e);
        }
    }

    public FileConfiguration getArenaConfig() {
        if (arenaConfig == null) {
            createArenaConfig();
        }
        return arenaConfig;
    }

    private int timerTaskId;
    private int timeLeft;
    private List<Player> playersAlive;
    private BukkitTask supplyDropTask;
    private BukkitTask chestRefillTask;
    private BukkitTask borderShrinkTask;

    public void startGame() {
        // Start game
        plugin.gameStarted = true;
        WorldBorderHandler worldBorderHandler = new WorldBorderHandler(plugin);
        worldBorderHandler.startBorderShrink();

        // Set the time left
        timeLeft = plugin.getConfig().getInt("game-time");

        // Initialize the list of players alive
        playersAlive = new ArrayList<>();

        // Get the arena region from the config
        FileConfiguration config = getArenaConfig();
        World world = plugin.getServer().getWorlds().get(0);
        String worldName = world.getName();
        
        double x1 = config.getDouble("arenas." + worldName + ".region.pos1.x");
        double y1 = config.getDouble("arenas." + worldName + ".region.pos1.y");
        double z1 = config.getDouble("arenas." + worldName + ".region.pos1.z");
        double x2 = config.getDouble("arenas." + worldName + ".region.pos2.x");
        double y2 = config.getDouble("arenas." + worldName + ".region.pos2.y");
        double z2 = config.getDouble("arenas." + worldName + ".region.pos2.z");

        Location minLocation = new Location(world, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
        Location maxLocation = new Location(world, Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));

        List<Player> notPlayingPlayers = new ArrayList<>();

        for (Player player : world.getPlayers()) {
            Location playerLocation = player.getLocation();
            if (playerLocation.getX() >= minLocation.getX() && playerLocation.getX() <= maxLocation.getX()
                    && playerLocation.getY() >= minLocation.getY() && playerLocation.getY() <= maxLocation.getY()
                    && playerLocation.getZ() >= minLocation.getZ() && playerLocation.getZ() <= maxLocation.getZ()
                    && player.getGameMode() == GameMode.ADVENTURE) {
                plugin.bossBar.addPlayer(player);
                playersAlive.add(player);
            } else {
                notPlayingPlayers.add(player);
            }
        }

        for (Player player : notPlayingPlayers) {
            player.setGameMode(GameMode.SPECTATOR);;
        }

        for (Player player : playersAlive) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "The game has started!");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "The grace period has started! PvP is disabled!");
            if (plugin.getConfig().getBoolean("bedrock-buff.enabled") && player.getName().startsWith(".")) {
                List<String> effectNames = plugin.getConfig().getStringList("bedrock-buff.effects");
                for (String effectName : effectNames) {
                    PotionEffectType effectType = PotionEffectType.getByName(effectName);
                    if (effectType != null) {
                        player.addPotionEffect(new PotionEffect(effectType, 200000, 1, true, false));
                    }
                }
            }
        }

        world.setPVP(false);
        int gracePeriod = plugin.getConfig().getInt("grace-period");
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            world.setPVP(true);
            for (Player player : playersAlive) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "The grace period has ended! PvP is now enabled!");
            }
        }, gracePeriod * 20L);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        assert manager != null;
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gameinfo", "dummy", "Game Info", RenderType.INTEGER);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score timeLeftScore = objective.getScore("Time Left:");
        timeLeftScore.setScore(timeLeft);
        Score playersAliveScore = objective.getScore("Players Alive:");
        playersAliveScore.setScore(playersAlive.size());
        Score worldBorderSizeScore = objective.getScore("World Border Size:");
        worldBorderSizeScore.setScore((int) world.getWorldBorder().getSize());

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Update the world border size score
            worldBorderSizeScore.setScore((int) world.getWorldBorder().getSize());
        }, 0L, 20L);

        for (Player player : playersAlive) {
            player.setScoreboard(scoreboard);
        }

        timerTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            plugin.bossBar.setProgress((double) timeLeft / plugin.getConfig().getInt("game-time"));
            timeLeft--;
            timeLeftScore.setScore(timeLeft);

            if (playersAlive.size() == 1) {
                plugin.getServer().getScheduler().cancelTask(timerTaskId);

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "The game has ended!");
                }

                Player winner = playersAlive.get(0);
                playersAlive.remove(0);
                updatePlayerCalificationScore(winner);
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + winner.getName() + " is the winner!");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
                endGame();
            }

            if (timeLeft < 0) {
                plugin.getServer().getScheduler().cancelTask(timerTaskId);
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePlayerCalificationScore(player);
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "The time is up! No one won the game!");
                }
                endGame();
            }
        }, 0L, 20L);
        int supplyDropInterval = plugin.getConfig().getInt("supplydrop.interval") * 20; // Convert seconds to ticks
        SupplyDropCommand supplyDropCommand = new SupplyDropCommand(plugin);
        PluginCommand supplyDropPluginCommand = plugin.getCommand("supplydrop");

        supplyDropTask = new BukkitRunnable() {
            @Override
            public void run() {
                assert supplyDropPluginCommand != null;
                supplyDropCommand.onCommand(plugin.getServer().getConsoleSender(), supplyDropPluginCommand, "supplydrop", new String[0]);
            }
        }.runTaskTimer(plugin, supplyDropInterval, supplyDropInterval);


        ChestRefillCommand chestRefillCommand = new ChestRefillCommand(plugin);
        PluginCommand chestRefillPluginCommand = plugin.getCommand("chestrefill");
        assert chestRefillPluginCommand != null;
        chestRefillCommand.onCommand(plugin.getServer().getConsoleSender(), chestRefillPluginCommand, "chestrefill", new String[0]);

        // Schedule a delayed task to refill chests again at specified time
        int chestRefillTime = plugin.getConfig().getInt("chestrefill.time") * 20; // Convert seconds to ticks
        chestRefillTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.gameStarted) {
                    chestRefillCommand.onCommand(plugin.getServer().getConsoleSender(), chestRefillPluginCommand, "chestrefill", new String[0]);
                }
            }
        }.runTaskLater(plugin, chestRefillTime);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (playersAlive != null) {
            plugin.bossBar.removePlayer(player);
            playersAlive.remove(player);
            World world = player.getWorld();
            assert world != null;
            Location spawnLocation = world.getSpawnLocation();
            player.teleport(spawnLocation);
            Map<Player, String> playerSpawnPoints = setSpawnHandler.getPlayerSpawnPoints();
            String spawnPoint = playerSpawnPoints.get(player);
            setSpawnHandler.removeOccupiedSpawnPoint(spawnPoint);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        World world = player.getWorld();
        if(!world.equals(plugin.getServer().getWorlds().get(0))){
            return;
        }
        Location spawnLocation = world.getSpawnLocation();
        player.teleport(spawnLocation);
        player.getInventory().clear();
        if(plugin.gameStarted){
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (playersAlive != null) {
            playersAlive.remove(player);
        }
        World world = player.getWorld();
        assert world != null;
        Location spawnLocation = world.getSpawnLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.spigot().respawn();
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.SPECTATOR);
        });
        Map<Player, String> playerSpawnPoints = setSpawnHandler.getPlayerSpawnPoints();
        String spawnPoint = playerSpawnPoints.get(player);
        setSpawnHandler.removeOccupiedSpawnPoint(spawnPoint);
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            List<Map<?, ?>> effectMaps = plugin.getConfig().getMapList("killer-effects");
            for (Map<?, ?> effectMap : effectMaps) {
                String effectName = (String) effectMap.get("effect");
                int duration = (int) effectMap.get("duration");
                int level = (int) effectMap.get("level");
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    killer.addPotionEffect(new PotionEffect(effectType, duration, level));
                }
            }
        }
        updatePlayerCalificationScore(player);
        updateKillerScore(killer);
        updatePlayersAliveScore();
    }

    public void updatePlayerCalificationScore(Player player){
        int maxScore = (int)plugin.getConfig().get("max-score");
        int minScore = (int)plugin.getConfig().get("min-score");
        String playerName = player.getName();
        int score = maxScore - playersAlive.size() < minScore ? minScore : maxScore - playersAlive.size();
        boolean found = false;
        List<Map<?,?>> rawPlayerList = getScoreData().getMapList("players");
        List<Map<String,Object>> playerList = convertToTypedMapList(rawPlayerList);
        for (Map<String,Object> map : playerList) {
            if(map.containsKey("name")&&map.get("name").equals(playerName)){
                found = true;
                ((List<Integer>)map.get("match")).add(score);
            }
        }
        if(!found){
            Map<String,Object> map = new TreeMap<>();
            map.put("name",playerName);
            map.put("kill",0);
            List<Integer> scores = new ArrayList<>();
            scores.add(score);
            map.put("match",scores);
            playerList.add(map);
        }
        getScoreData().set("players", playerList);
        saveScoreData();
    }

    public void updateKillerScore(Player player){
        int killScore = (int)plugin.getConfig().get("kill-score");
        String playerName = player.getName();
        boolean found = false;
        List<Map<?,?>> rawPlayerList = getScoreData().getMapList("players");
        List<Map<String,Object>> playerList = convertToTypedMapList(rawPlayerList);
        for (Map<String,Object> map : playerList) {
            if(map.containsKey("name")&&map.get("name").equals(playerName)){
                found = true;
                int value = (int)map.get("kill");
                map.replace("kill",value+killScore);
            }
        }
        if(!found){
            Map<String,Object> map = new TreeMap<>();
            map.put("name",playerName);
            map.put("kill",killScore);
            List<Integer> scores = new ArrayList<>();
            map.put("match",scores);
            playerList.add(map);
        }
        getScoreData().set("players", playerList);
        saveScoreData();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> convertToTypedMapList(List<Map<?, ?>> rawList) {
        // Perform the conversion with appropriate casting
        return (List<Map<String, Object>>) (List<?>) rawList;
    }

    public void updatePlayersAliveScore() {
        for (Player player : playersAlive) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getObjective("gameinfo");
            assert objective != null;
            Score playersAliveScore = objective.getScore("Players Alive:");
            playersAliveScore.setScore(playersAlive.size());
        }
    }


    public void endGame() {
        plugin.gameStarted = false;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        World world = plugin.getServer().getWorlds().get(0);
        WorldBorderHandler worldBorderHandler = new WorldBorderHandler(plugin);
        worldBorderHandler.initializeBorder();
        Server server = plugin.getServer();

        if (!world.getEntitiesByClass(Item.class).isEmpty()) {
            server.dispatchCommand(server.getConsoleSender(), "kill @e[type=item]");
        }

        if (!world.getEntitiesByClass(ExperienceOrb.class).isEmpty()) {
            server.dispatchCommand(server.getConsoleSender(), "kill @e[type=experience_orb]");
        }

        if (!world.getEntitiesByClass(Arrow.class).isEmpty()) {
            server.dispatchCommand(server.getConsoleSender(), "kill @e[type=arrow]");
        }

        if (!world.getEntitiesByClass(Trident.class).isEmpty()) {
            server.dispatchCommand(server.getConsoleSender(), "kill @e[type=trident]");
        }

        plugin.getServer().getScheduler().cancelTask(timerTaskId);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        assert manager != null;
        Scoreboard emptyScoreboard = manager.getNewScoreboard();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.bossBar.removePlayer(player);
            player.setScoreboard(emptyScoreboard);
        }
        playersAlive.clear();

        setSpawnHandler.clearOccupiedSpawnPoints();

        Location spawnLocation = world.getSpawnLocation();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.teleport(spawnLocation);
            player.getInventory().clear();
        }
        world.setPVP(false);

        if (supplyDropTask != null) {
            supplyDropTask.cancel();
            supplyDropTask = null;
        }

        if (chestRefillTask != null) {
            chestRefillTask.cancel();
            chestRefillTask = null;
        }

        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
            borderShrinkTask = null;
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof ShulkerBox shulkerBox) {
                    if (shulkerBox.getColor() == DyeColor.RED) {
                        shulkerBox.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FLINT){
            updateKillerScore(player);
        }
    }
}