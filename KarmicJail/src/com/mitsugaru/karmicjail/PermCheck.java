package com.mitsugaru.karmicjail;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Class to handle permission node checks.
 * Mostly only to support PEX natively, due to
 * SuperPerm compatibility with PEX issues.
 *
 * @author Mitsugaru
 *
 */
public class PermCheck {
	private Permission perm;
	private boolean hasVault;

	/**
	 * Constructor
	 */
	public PermCheck(KarmicJail kj)
	{
		if(kj.getServer().getPluginManager().getPlugin("Vault") != null)
		{
			hasVault = true;
			RegisteredServiceProvider<Permission> permissionProvider = kj
				.getServer()
				.getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
			if (permissionProvider != null)
			{
				perm = permissionProvider.getProvider();
			}
		}
		else
		{
			hasVault = false;
		}

	}

	/**
	 *
	 * @param CommandSender that sent command
	 * @param Permission node to check, as String
	 * @return true if sender has the node, else false
	 */
	public boolean has(CommandSender sender, String node)
	{
		//Pex specific supercedes vault
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx"))
		{
			//Pex only supports player check, no CommandSender objects
			if(sender instanceof Player)
			{
				final Player p = (Player) sender;
				final PermissionManager permissions = PermissionsEx.getPermissionManager();
				//Handle pex check
				if(permissions.has(p, node))
				{
					return true;
				}
			}
		}
		//Use vault if we have it
		if(hasVault)
		{
			return perm.has(sender, node);
		}
		//If not using PEX / Vault, OR if sender is not a player (in PEX only case)
		//Attempt to use SuperPerms
		if(sender.hasPermission(node))
		{
			return true;
		}
		//Else, they don't have permission
		return false;
	}

	public void playerAddGroup(World world, String name, String group) {
		if(hasVault)
		{
			perm.playerAddGroup(world, name, group);
		}
	}

	public String[] getPlayerGroups(World w, String name) {
		String[] groups = new String[0];
		if(hasVault)
		{
			groups = perm.getPlayerGroups(w, name);
		}
		return groups;
	}

	public void playerRemoveGroup(World w, String name, String group) {
		if(hasVault)
		{
			perm.playerRemoveGroup(w, name, group);
		}
	}

	public void playerAddGroup(String world, String name, String group) {
		if(hasVault)
		{
			perm.playerAddGroup(world, name, group);
		}
	}
}
