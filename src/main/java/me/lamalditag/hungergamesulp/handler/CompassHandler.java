package me.lamalditag.hungergamesulp.handler;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.lamalditag.hungergamesulp.HungerGamesULP;

public class CompassHandler {

    private final HungerGamesULP plugin;

    public CompassHandler(HungerGamesULP plugin) {
        this.plugin = plugin;

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : plugin.playersAlive) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.COMPASS) {
                    Player nearestPlayer = findNearestPlayer(player);
                    if (nearestPlayer != null) {
                        player.setCompassTarget(nearestPlayer.getLocation());
                    }
                }
            }
        }, 0L, 20L);
    }

    private Player findNearestPlayer(Player player) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;
        for (Player otherPlayer : plugin.playersAlive) {
            if (otherPlayer != player && otherPlayer.getWorld() == player.getWorld()) {
                double distance = player.getLocation().distance(otherPlayer.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = otherPlayer;
                }
            }
        }
        return closestPlayer;
    }
}
