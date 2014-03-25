package com.mitsugaru.karmicjail.interfaces;

import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.services.Module;
import com.mitsugaru.karmicjail.services.PermissionNode;

public interface IPermissionModule extends Module  {

    /**
     * Checks if the sender has the correct permission. Should be as simple as
     * returning has(sender, permission.getNode());
     * 
     * @param sender
     *            - Command sender.
     * @param permission
     *            - Permission Node.
     * @return True if sender has permission, else false.
     */
    boolean has(CommandSender sender, PermissionNode permission);

    /**
     * Vault style checking. Used for seeing if a player is exempt from being
     * jailed.
     * 
     * @param name
     *            - Name of player.
     * @param permission
     *            - Permission node.
     * @return True if sender has permission, else false.
     */
    boolean has(String name, PermissionNode permission);

}
