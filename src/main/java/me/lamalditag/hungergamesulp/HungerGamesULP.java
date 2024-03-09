package me.lamalditag.hungergamesulp;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.lamalditag.hungergamesulp.commands.*;
import me.lamalditag.hungergamesulp.handler.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class HungerGamesULP extends JavaPlugin {

    public HungerGamesULP() {

    }

    public boolean gameStarted = false;
    public BossBar bossBar;
    private GameHandler gameHandler;
    public List<Player> playersAlive;
    private SetSpawnHandler setSpawnHandler;
    private WorldBorderHandler worldBorderHandler;
    private ChestRefillCommand chestRefillCommand;

    @Override
    public void onEnable() {
        bossBar = getServer().createBossBar("Time Remaining", BarColor.BLUE, BarStyle.SOLID);
        setSpawnHandler = new SetSpawnHandler(this);
        worldBorderHandler = new WorldBorderHandler(this);
        gameHandler = new GameHandler(this, setSpawnHandler);
        playersAlive = new ArrayList<>();
        new CompassHandler(this);
        chestRefillCommand = new ChestRefillCommand(this);

        worldBorderHandler.initializeBorder();

        saveDefaultConfig();
        saveResource("items.yml", false);
        Objects.requireNonNull(getCommand("supplydrop")).setExecutor(new SupplyDropCommand(this));
        Objects.requireNonNull(getCommand("create")).setExecutor(new ArenaSelectorCommand(this));
        Objects.requireNonNull(getCommand("select")).setExecutor(new ArenaSelectorCommand(this));
        Objects.requireNonNull(getCommand("setspawn")).setExecutor(new SetSpawnCommand(this));
        Objects.requireNonNull(getCommand("chestrefill")).setExecutor(new ChestRefillCommand(this));
        Objects.requireNonNull(getCommand("start")).setExecutor(new StartGameCommand(this));
        Objects.requireNonNull(getCommand("end")).setExecutor(new EndGameCommand(this));
        Objects.requireNonNull(getCommand("scanarena")).setExecutor(new ScanArenaCommand(this));
        MoveToggleCommand moveToggleCommand = new MoveToggleCommand(this, chestRefillCommand);
        Objects.requireNonNull(getCommand("move-toggle")).setExecutor(moveToggleCommand);
        BorderSetCommand borderSetCommand = new BorderSetCommand(this);
        Objects.requireNonNull(getCommand("border")).setExecutor(borderSetCommand);
        Objects.requireNonNull(getCommand("border")).setTabCompleter(borderSetCommand);
        getServer().getPluginManager().registerEvents(new SetArenaHandler(this), this);
        getServer().getPluginManager().registerEvents(setSpawnHandler, this);
        getServer().getPluginManager().registerEvents(worldBorderHandler, this);
        getServer().getPluginManager().registerEvents(gameHandler, this);
        getServer().getPluginManager().registerEvents(moveToggleCommand, this);
    }

    public GameHandler getGameHandler() {
        return gameHandler;
    }

    public SetSpawnHandler getSetSpawnHandler() {
        return setSpawnHandler;
    }
}
