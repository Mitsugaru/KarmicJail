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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class KarmicJailListener implements Listener {
	//Class variables
    private final KarmicJail plugin;
    private static final long minutesToTicks = 1200;

    public KarmicJailListener(KarmicJail plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {

        final Player player = event.getPlayer();

        if(!plugin.playerIsJailed(player.getName())) return;

        event.setRespawnLocation(plugin.getJailLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {

    	//Attempt to add player to database
    	plugin.addPlayerToDatabase(event.getPlayer().getName());

        final Player player = event.getPlayer();
        final String status = plugin.getPlayerStatus(player.getName());

        // Check status
        if (status.equals(""+JailStatus.PENDINGJAIL) || status.equals(""+JailStatus.JAILED)) {
            if (plugin.playerIsTempJailed(player.getName())) {
            	final long time = plugin.getPlayerTime(player.getName());
                final int minutes = (int) ((time / minutesToTicks));
                player.sendMessage(ChatColor.AQUA + "You are jailed for " + plugin.prettifyMinutes(minutes) + ".");
                plugin.addThread(player.getName(), time);
            } else {
                player.sendMessage(ChatColor.AQUA + "You are jailed.");
            }
            player.teleport(plugin.getJailLocation());
            if(status.equals(""+JailStatus.PENDINGJAIL))
            {
            	plugin.setPlayerStatus(JailStatus.JAILED, player.getName());
            }

        } else if (status.equals(""+JailStatus.PENDINGFREE)) {
            plugin.setPlayerStatus(JailStatus.FREED, player.getName());
            plugin.teleportOut(player.getName());
            player.sendMessage(ChatColor.AQUA + "You have been removed from jail.");
        }
        //TODO if they are free, and if they contain the group Jailed, remove it.
    }

    @EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event)
    {
    	final Player player = event.getPlayer();
    	if(!plugin.playerIsJailed(player.getName())) return;

        if (plugin.playerIsTempJailed(player.getName())) {
        	plugin.stopTask(player.getName());
        }
    }

}
