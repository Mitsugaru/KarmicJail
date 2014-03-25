package com.mitsugaru.karmicjail.command.shortcut;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.services.CommandHandler;

public class UnjailShortcut extends CommandHandler {

    public UnjailShortcut(KarmicJail plugin, String cmd) {
        super(plugin, "unjail");
    }

    @Override
    public boolean noArgs(CommandSender sender, Command command, String label) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean unknownCommand(CommandSender sender, Command command,
            String label, String[] args) {
        // TODO Auto-generated method stub
        return true;
    }

}
