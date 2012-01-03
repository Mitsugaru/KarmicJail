/**
 * Player listener.
 * Built upon the SimpleJail project, created by imjake9.
 * https://github.com/imjake9/SimpleJail
 *
 * @author imjake9
 * @author Mitsugaru
 */
package com.mitsugaru.karmicjail;

import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class Listener extends PlayerListener {
	//Class variables
    private final KarmicJail plugin;
    private static final long minutesToTicks = 1200;

    public Listener(KarmicJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerChat(final PlayerChatEvent event)
    {
    	final Player player = event.getPlayer();
        if(plugin.playerIsJailed(player.getName()))
        {
        	if(plugin.playerIsMuted(player.getName()))
        	{
        		event.setCancelled(true);
        	}
        }
    }

    @Override
    public void onPlayerRespawn(final PlayerRespawnEvent event) {

        final Player player = event.getPlayer();

        if(!plugin.playerIsJailed(player.getName())) return;

        event.setRespawnLocation(plugin.getJailLocation());
    }

    @Override
    public void onPlayerJoin(final PlayerJoinEvent event) {

    	//Attempt to add player to database
    	plugin.addPlayerToDatabase(event.getPlayer().getName());

        final Player player = event.getPlayer();
        final String status = plugin.getPlayerStatus(player.getName());

        // Check status
        if (status.equals(""+JailStatus.PENDINGJAIL)) {
            if (plugin.playerIsTempJailed(player.getName())) {
                int minutes = (int) ((plugin.getPlayerTime(player.getName()) / minutesToTicks));
                player.sendMessage(ChatColor.AQUA + "You are jailed for " + plugin.prettifyMinutes(minutes) + ".");
                plugin.addThread(player.getName());
            } else {
                player.sendMessage(ChatColor.AQUA + "You are jailed.");
            }
            player.teleport(plugin.getJailLocation());
            plugin.setPlayerStatus(JailStatus.JAILED, player.getName());

        } else if (status.equals(""+JailStatus.PENDINGFREE)) {
            plugin.setPlayerStatus(JailStatus.FREED, player.getName());
            plugin.teleportOut(player.getName());
            player.sendMessage(ChatColor.AQUA + "You have been removed from jail!");
        }
        else if(status.equals(""+JailStatus.JAILED) && plugin.playerIsTempJailed(player.getName()))
        {
        	plugin.addThread(player.getName());
        }
    }

	@Override
	public void onPlayerQuit(final PlayerQuitEvent event)
    {
    	final Player player = event.getPlayer();
    	if(!plugin.playerIsJailed(player.getName())) return;

        if (plugin.playerIsTempJailed(player.getName())) {
        	plugin.stopTask(player.getName());
        }
    }

}
