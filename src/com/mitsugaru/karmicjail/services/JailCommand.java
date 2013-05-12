package com.mitsugaru.karmicjail.services;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;

/**
 * Represents a command.
 * 
 * @param <T>
 *           Game plugin.
 */
public interface JailCommand {

   /**
    * Execution method for the command.
    * 
    * @param sender
    *           - Sender of the command.
    * @param command
    *           - Command used.
    * @param label
    *           - Label.
    * @param args
    *           - Command arguments.
    * @return True if valid command and executed. Else false.
    */
   boolean execute(final KarmicJail plugin, final CommandSender sender, final Command command, final String label, String[] args);

}
