/**
 * Jail plugin custom tailed for the needs of Mine-RP. Built upon the SimpleJail
 * project, created by imjake9. https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */

package com.mitsugaru.karmicjail;

import java.util.HashMap;
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
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.services.CommandHandler;
import com.mitsugaru.karmicjail.services.JailModule;
import com.mitsugaru.karmicjail.tasks.JailTask;
import com.mitsugaru.karmicjail.update.Update;

public class KarmicJail extends JavaPlugin {
   /**
    * Plugin tag.
    */
   public static final String TAG = "[KarmicJail]";
   /**
    * Minutes to ticks ratio.
    */
   public static final long minutesToTicks = 1200;
   private static final Map<String, JailTask> threads = new HashMap<String, JailTask>();
   /**
    * Modules.
    */
   private final Map<Class<? extends JailModule>, JailModule> modules = new HashMap<Class<? extends JailModule>, JailModule>();

   private final Map<Class<? extends CommandHandler>, CommandHandler> handlers = new HashMap<Class<? extends CommandHandler>, CommandHandler>();

   @Override
   public void onDisable() {
      // Stop all running threads
      getLogger().info("Stopping all jail threads...");
      for(JailTask task : threads.values()) {
         task.stop();
      }
   }

   @Override
   public void onEnable() {
      // Register config
      registerModule(RootConfig.class, new RootConfig(this));
      // Register database
      registerModule(DBHandler.class, new DBHandler(this));
      // Initialize logic
      registerModule(JailLogic.class, new JailLogic(this));
      // Register permissions
      registerModule(PermCheck.class, new PermCheck(this));

      // Check if any updates are necessary
      Update.init(this);
      Update.checkUpdate();

      // Generate commanders.
      Commander commander = new Commander(this);
      getCommand("kj").setExecutor(commander);
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
    * Creates a new thread for a player to get auto-released
    * 
    * @param name
    *           of player
    */
   public void addThread(String name, long time) {
      threads.put(name, new JailTask(this, name, time));
   }

   /**
    * Removes a task
    * 
    * @param name
    */
   public static void removeTask(String name) {
      threads.remove(name);
   }

   /**
    * Stops a player's timed task
    * 
    * @param name
    *           of player
    * @return true if the player's task was stopped. If unsucessful or if player
    *         did not have a timed task, then it returns false.
    */
   public boolean stopTask(String name) {
      RootConfig config = getModuleForClass(RootConfig.class);
      if(threads.containsKey(name)) {
         if(config.debugLog && config.debugEvents) {
            this.getLogger().info("Thread found for: " + name);
         }
         final boolean stop = threads.get(name).stop();
         if(config.debugLog && config.debugEvents) {
            if(stop) {
               this.getLogger().info("Thread stopped for: " + name);
            } else {
               this.getLogger().warning("Thread NOT stopped for: " + name);
            }
         }
         return stop;
      } else {
         if(config.debugLog && config.debugEvents) {
            this.getLogger().warning("Thread NOT found for: " + name);
         }
      }
      return false;
   }

   public static Map<String, JailTask> getJailThreads() {
      return threads;
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
   public <T extends JailModule> void registerModule(Class<T> clazz, T module) {
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
   public <T extends JailModule> T deregisterModuleForClass(Class<T> clazz) {
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
   public <T extends JailModule> T getModuleForClass(Class<T> clazz) {
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
