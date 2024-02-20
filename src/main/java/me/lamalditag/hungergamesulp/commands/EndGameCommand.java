package me.lamalditag.hungergamesulp.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import me.lamalditag.hungergamesulp.HungerGamesULP;

public class EndGameCommand implements CommandExecutor {
    private final HungerGamesULP plugin;

    public EndGameCommand(HungerGamesULP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!plugin.gameStarted) {
            sender.sendMessage(ChatColor.RED + "The game has not started yet!");
            return true;
        }

        plugin.getGameHandler().endGame();
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "The game has ended!");
        return true;
    }
}
