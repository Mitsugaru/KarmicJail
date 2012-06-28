package com.mitsugaru.karmicjail.tasks;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LoginWarpTask implements Runnable {

    private Player player;
    private Location location;

    public LoginWarpTask(Player player, Location location) {
	this.player = player;
	this.location = location;
    }

    @Override
    public void run() {
	player.teleport(location);
    }

}
