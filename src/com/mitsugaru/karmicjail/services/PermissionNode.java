package com.mitsugaru.karmicjail.services;

/**
 * Represents all possible permission nodes for the plugin.
 */
public enum PermissionNode {
   JAIL(".jail"),
   TIMED(".timed"),
   UNJAIL(".unjail"),
   SETJAIL(".setjail"),
   LIST(".list"),
   MUTE(".mute"),
   INVENTORY_VIEW(".inventory.view"),
   INVENTORY_MODIFY(".inventory.modify"),
   WARP_JOINIGNORE(".warp.joinignore"),
   WARP_JAIL(".warp.jail"),
   WARP_LAST(".warp.last"),
   HISTORY_VIEW(".history.view"),
   HISTORY_ADD(".history.add"),
   JAILSTATUS(".jailstatus"),
   BROADCAST(".broadcast"),
   EXEMPT(".exempt");

   /**
    * Plugin prefix.
    */
   private static final String prefix = "KarmicJail";
   /**
    * Node path.
    */
   private String node;

   /**
    * Private constructor.
    * 
    * @param node
    *           - Sub path of permission node.
    */
   private PermissionNode(String node) {
      this.node = prefix + node;
   }

   /**
    * Get the Permision node.
    * 
    * @return Full permission node.
    */
   public String getNode() {
      return node;
   }
}
