/**
 * Jail plugin custom tailed for the needs of Mine-RP. Built upon the SimpleJail
 * project, created by imjake9. https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */

package com.mitsugaru.karmicjail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mitsugaru.karmicjail.command.Commander;
import com.mitsugaru.karmicjail.command.history.HistoryCommander;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.listener.JailedInventoryListener;
import com.mitsugaru.karmicjail.listener.JailedPlayerListener;
import com.mitsugaru.karmicjail.listener.PlayerListener;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.modules.TaskModule;
import com.mitsugaru.karmicjail.modules.UpdateModule;
import com.mitsugaru.karmicjail.modules.UtilityModule;
import com.mitsugaru.karmicjail.services.CommandHandler;
import com.mitsugaru.karmicjail.services.Module;
import com.mitsugaru.karmicjail.services.ServiceComparator;

public class KarmicJail extends JavaPlugin {
   /**
    * Plugin tag.
    */
   public static final String TAG = "[KarmicJail]";
   /**
    * Minutes to ticks ratio.
    */
   public static final long minutesToTicks = 1200;
   /**
    * Modules.
    */
   private final Map<Class<? extends Module>, Module> modules = new HashMap<Class<? extends Module>, Module>();

   private final Map<Class<? extends CommandHandler>, CommandHandler> handlers = new HashMap<Class<? extends CommandHandler>, CommandHandler>();

   @Override
   public void onDisable() {
    // Deregister all modules.
       List<Class<? extends Module>> clazzez = new ArrayList<Class<? extends Module>>();
       clazzez.addAll(modules.keySet());
       Collections.sort(clazzez, new ServiceComparator<Object>());
       for(Class<? extends Module> clazz : clazzez) {
           this.deregisterModuleForClass(clazz);
       }
   }

   @Override
   public void onEnable() {
      // Register config
      registerModule(RootConfig.class, new RootConfig(this));
      // Register database
      registerModule(DBHandler.class, new DBHandler(this));
      // Register tasker
      registerModule(TaskModule.class, new TaskModule(this));
      // Register utility
      registerModule(UtilityModule.class, new UtilityModule(this));
      // Initialize logic
      registerModule(JailLogic.class, new JailLogic(this));
      // Register permissions
      registerModule(PermCheck.class, new PermCheck(this));

      // Check if any updates are necessary
      registerModule(UpdateModule.class, new UpdateModule(this));

      // Generate commanders.
      Commander commander = new Commander(this);
      getCommand("kj").setExecutor(commander);
      if(getDescription().getCommands().containsKey("jail")) {
          //TODO add in jail shortcut command
      }
      if(getDescription().getCommands().containsKey("unjail")) {
          //TODO add in unjail shortcut command
      }
      HistoryCommander history = new HistoryCommander(this);
      commander.registerHandler(history);
      // Register commanders.
      handlers.put(Commander.class, commander);
      handlers.put(HistoryCommander.class, history);

      // Setup listeners
      final PluginManager pm = this.getServer().getPluginManager();
      pm.registerEvents(new PlayerListener(this), this);
      pm.registerEvents(new JailedInventoryListener(this), this);
      pm.registerEvents(new JailedPlayerListener(this), this);
   }

   /**
    * Makes the minutes readable
    * 
    * @param minutes
    * @return String of readable minutes
    */
   public String prettifyMinutes(int minutes) {
      if(minutes < 1) {
         return "about less than a minute";
      }
      if(minutes == 1)
         return "about one minute";
      if(minutes < 60)
         return "about " + minutes + " minutes";
      if(minutes % 60 == 0) {
         if(minutes / 60 == 1)
            return "about one hour";
         else
            return "about " + (minutes / 60) + " hours";
      }
      int m = minutes % 60;
      int h = (minutes - m) / 60;
      return "about " + h + "h" + m + "m";
   }

   /**
    * Attempts to look up full name based on who's on the server Given a partial
    * name
    * 
    * @author Frigid, edited by Raphfrk and petteyg359
    */
   public String expandName(String Name) {
      int m = 0;
      String Result = "";
      for(int n = 0; n < this.getServer().getOnlinePlayers().length; n++) {
         String str = this.getServer().getOnlinePlayers()[n].getName();
         if(str.matches("(?i).*" + Name + ".*")) {
            m++;
            Result = str;
            if(m == 2) {
               return null;
            }
         }
         if(str.equalsIgnoreCase(Name))
            return str;
      }
      if(m == 1)
         return Result;
      if(m > 1) {
         return null;
      }
      return Name;
   }

   /**
    * Register a CCModule to the API.
    * 
    * @param clazz
    *           - Class of the instance.
    * @param module
    *           - Module instance.
    * @throws IllegalArgumentException
    *            - Thrown if an argument is null.
    */
   public <T extends Module> void registerModule(Class<T> clazz, T module) {
      // Check arguments.
      if(clazz == null) {
         throw new IllegalArgumentException("Class cannot be null");
      } else if(module == null) {
         throw new IllegalArgumentException("Module cannot be null");
      }
      // Add module.
      modules.put(clazz, module);
      // Tell module to start.
      module.starting();
   }

   /**
    * Unregister a CCModule from the API.
    * 
    * @param clazz
    *           - Class of the instance.
    * @return Module that was removed from the API. Returns null if no instance
    *         of the module is registered with the API.
    */
   public <T extends Module> T deregisterModuleForClass(Class<T> clazz) {
      // Check arguments.
      if(clazz == null) {
         throw new IllegalArgumentException("Class cannot be null");
      }
      // Grab module and tell it its closing.
      T module = clazz.cast(modules.get(clazz));
      if(module != null) {
         module.closing();
      }
      return module;
   }

   /**
    * Retrieve a registered CCModule.
    * 
    * @param clazz
    *           - Class identifier.
    * @return CCModule instance. Returns null is an instance of the given class
    *         has not been registered with the API.
    */
   public <T extends Module> T getModuleForClass(Class<T> clazz) {
      return clazz.cast(modules.get(clazz));
   }

   /**
    * Get the command handler.
    * 
    * @param clazz
    *           - Class identifier.
    * @return CommandHandler instance.
    */
   public <T extends CommandHandler> T getCommandHandlerForClass(Class<T> clazz) {
      return clazz.cast(handlers.get(clazz));
   }

}
