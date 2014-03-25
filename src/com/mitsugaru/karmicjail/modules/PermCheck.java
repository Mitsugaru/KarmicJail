package com.mitsugaru.karmicjail.modules;

import net.milkbowl.vault.permission.Permission;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.interfaces.IPermissionModule;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.services.AbstractModule;
import com.mitsugaru.karmicjail.services.PermissionNode;
import com.mitsugaru.karmicjail.services.Service;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Class to handle permission node checks. Mostly only to support PEX natively,
 * due to SuperPerm compatibility with PEX issues.
 */
@Service
public class PermCheck extends AbstractModule implements IPermissionModule {
    private Permission perm;
    private boolean hasVault;
    private String pluginName;

    /**
     * Constructor
     */
    public PermCheck(KarmicJail kj) {
        super(kj);
    }

    @Override
    public boolean has(CommandSender sender, PermissionNode permission) {
        return has(sender, permission.getNode());
    }

    @Override
    public boolean has(String name, PermissionNode permission) {
        JailLogic logic = plugin.getModuleForClass(JailLogic.class);
        if(hasVault) {
            return perm.has(logic.getJailLocation().getWorld(), name,
                    permission.getNode());
        }
        return false;
    }

    /**
     * 
     * @param CommandSender
     *            that sent command
     * @param PermissionNode
     *            node to check, as String
     * @return true if sender has the node, else false
     */
    private boolean has(CommandSender sender, String node) {
        boolean permission = false;
        // Use vault if we have it, else use super perms
        if(hasVault) {
            permission = perm.has(sender, node);
        } else {
            permission = sender.hasPermission(node); 
        }
        return permission;
    }

    public String getDefaultGroup() {
        String def = "default";
        if(hasVault) {
            if(pluginName.equals("PermissionsEx")) {
                for(PermissionGroup group : PermissionsEx
                        .getPermissionManager().getGroups()) {
                    try {
                        if(group.isDefault(this.plugin.getServer().getWorlds()
                                .get(0).toString())) {
                            def = group.getName();
                        }
                    } catch(IndexOutOfBoundsException e) {
                        this.plugin.getLogger().warning(
                                KarmicJail.TAG + " Cannot grab default group.");
                        return def;
                    }
                }
            } else if(pluginName.equals("PermissionsBukkit")) {
                // Last I remember, PermissionsBukkit forces the default group
                // to be named default
                return def;
            } else if(pluginName.equals("bPermissions2")
                    || pluginName.equals("bPermissions")) {
                // IDEK anymore
                return def;
            } else if(pluginName.equals("GroupManager")) {
                def = ((GroupManager) this.plugin.getServer()
                        .getPluginManager().getPlugin("GroupManager"))
                        .getWorldsHolder()
                        .getWorldData(
                                this.plugin.getServer().getWorlds().get(0)
                                        .toString()).getDefaultGroup()
                        .getName();
            }
        }
        return def;
    }

    public String[] getPlayerGroups(World w, String name) {
        String[] groups = new String[0];
        if(hasVault) {
            groups = perm.getPlayerGroups(w, name);
        }
        return groups;
    }

    public void playerRemoveGroup(World w, String name, String group) {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(hasVault) {
            if(pluginName.equals("PermissionsBukkit")) {
                // Handle PermissionsBukkit
                final String cmd = "permissions player removegroup " + name
                        + " " + group;
                final boolean check = plugin.getServer().dispatchCommand(
                        Bukkit.getServer().getConsoleSender(), cmd);
                if(!check) {
                    plugin.getLogger().warning(
                            "Could not remove group '" + group + "' from '"
                                    + name + "'... Permissions error.");
                } else if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Removed group '" + group + "' of '" + w
                                    + "' from '" + name + "'");
                }
            } else if(pluginName.equals("PermissionsEX")) {
                final PermissionManager pm = PermissionsEx
                        .getPermissionManager();
                final PermissionUser user = pm.getUser(name);
                final PermissionGroup permGroup = PermissionsEx
                        .getPermissionManager().getGroup(group);
                user.removeGroup(permGroup, w.getName());
                if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Removed group '" + group + "' of '" + w
                                    + "' from '" + name + "'");
                }
            } else {
                final boolean check = perm.playerRemoveGroup(w, name, group);
                if(!check) {
                    plugin.getLogger().warning(
                            "Could not remove group '" + group + "' of world '"
                                    + w.getName() + "' from '" + name
                                    + "'... Permissions error.");
                } else if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Removed group '" + group + "' of '" + w
                                    + "' from '" + name + "'");
                }
            }
        }
    }

    public void playerAddGroup(String world, String name, String group) {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(hasVault) {
            if(pluginName.equals("PermissionsBukkit")) {
                // Handle PermissionsBukkit
                final String cmd = "permissions player addgroup " + name + " "
                        + group;
                final boolean check = plugin.getServer().dispatchCommand(
                        Bukkit.getServer().getConsoleSender(), cmd);
                if(!check) {
                    plugin.getLogger().warning(
                            "Could not add group '" + group + "' to '" + name
                                    + "' = Permissions error.");
                } else if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Added group '" + group + "' of '" + world
                                    + "' to '" + name + "'");
                }
            } else if(pluginName.equals("PermissionsEX")) {
                final PermissionManager pm = PermissionsEx
                        .getPermissionManager();
                final PermissionUser user = pm.getUser(name);
                final PermissionGroup permGroup = PermissionsEx
                        .getPermissionManager().getGroup(group);
                user.addGroup(permGroup);
                if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Added group '" + group + "' of '" + world
                                    + "' to '" + name + "'");
                }
            } else {
                final boolean check = perm.playerAddGroup(world, name, group);
                if(!check) {
                    plugin.getLogger().warning(
                            "Could not add group '" + group + "' of world '"
                                    + world + "' to '" + name
                                    + "' = Permissions error.");
                } else if(config.debugGroups) {
                    plugin.getLogger().info(
                            "Added group '" + group + "' of '" + world
                                    + "' to '" + name + "'");
                }
            }
        }
    }

    public String getName() {
        return perm.getName();
    }

    @Override
    public void starting() {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            hasVault = true;
            RegisteredServiceProvider<Permission> permissionProvider = plugin
                    .getServer()
                    .getServicesManager()
                    .getRegistration(
                            net.milkbowl.vault.permission.Permission.class);
            if(permissionProvider != null) {
                perm = permissionProvider.getProvider();
                pluginName = perm.getName();
                if(config.debugGroups) {
                    plugin.getLogger().info("Using permissions: " + pluginName);
                }
            }
        } else {
            hasVault = false;
        }
    }

    @Override
    public void closing() {
    }
}
