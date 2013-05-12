package com.mitsugaru.karmicjail.command;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.services.JailCommand;

public class VersionCommand implements JailCommand {
   
   private static final String bar = "======================";

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      // Version
      sender.sendMessage(ChatColor.BLUE + "==================" + ChatColor.GREEN + "KarmicJail v" + plugin.getDescription().getVersion()
            + ChatColor.BLUE + "=================");
      sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru" + ChatColor.WHITE + " - " + ChatColor.AQUA + "Fork of imjake9's SimpleJail project");
      sender.sendMessage(ChatColor.WHITE + "Shout outs: " + ChatColor.GOLD + "@khanjal");
      sender.sendMessage(ChatColor.BLUE + bar + ChatColor.GRAY + "Config" + ChatColor.BLUE + bar);
      DecimalFormat twoDForm = new DecimalFormat("#.##");
      sender.sendMessage(ChatColor.LIGHT_PURPLE + "Jail: " + ChatColor.GRAY + config.jailLoc.getWorld().getName() + ChatColor.BLUE + " : ("
            + ChatColor.WHITE + Double.valueOf(twoDForm.format(config.jailLoc.getX())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.jailLoc.getY())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.jailLoc.getZ())) + ChatColor.BLUE + ")");
      sender.sendMessage(ChatColor.LIGHT_PURPLE + "UnJail: " + ChatColor.GRAY + config.unjailLoc.getWorld().getName() + ChatColor.BLUE + " : ("
            + ChatColor.WHITE + Double.valueOf(twoDForm.format(config.unjailLoc.getX())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.unjailLoc.getY())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.unjailLoc.getZ())) + ChatColor.BLUE + ")");
      return true;
   }

}
