package com.mitsugaru.karmicjail.command.history;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.services.CommandHandler;
import com.mitsugaru.karmicjail.services.JailCommand;

public class HistoryCommander extends CommandHandler {

   private final Map<String, Integer> page = new HashMap<String, Integer>();
   private final Map<String, String> cache = new HashMap<String, String>();

   public HistoryCommander(KarmicJail plugin) {
      super(plugin, "history");
      // Register commands
      registerCommand("next", new NextHistoryCommand());
      registerCommand("prev", new PrevHistoryCommand());
      registerCommand("page", new PageHistoryCommand());
      registerCommand("add", new AddHistoryCommand());
      HelpCommand help = new HelpCommand();
      registerCommand("help", help);
      registerCommand("?", help);
      registerCommand("view", new ViewHistoryCommand());
   }

   @Override
   public boolean noArgs(CommandSender sender, Command command, String label) {
      sender.sendMessage(ChatColor.GREEN + "/kj history view" + ChatColor.YELLOW + " : Show currently open history");
      sender.sendMessage(ChatColor.GREEN + "/kj history" + ChatColor.AQUA + " <prev | next>" + ChatColor.YELLOW
            + " : Go to previous or next page of history");
      sender.sendMessage(ChatColor.GREEN + "/kj history" + ChatColor.AQUA + " view <player>" + ChatColor.YELLOW + " : View history of given player");
      sender.sendMessage(ChatColor.GREEN + "/kj history" + ChatColor.AQUA + " page <#>" + ChatColor.YELLOW + " : Go to given page number of history");
      sender.sendMessage(ChatColor.GREEN + "/kj history" + ChatColor.AQUA + " add <player> <comment...>" + ChatColor.YELLOW
            + " : Add a comment to the history of a given player");
      return true;
   }

   @Override
   public boolean unknownCommand(CommandSender sender, Command command, String label, String[] args) {
      sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Invalid history command '" + args[0] + "', use /jhistory help.");
      return true;
   }

   public Map<String, Integer> getPage() {
      return page;
   }

   public Map<String, String> getCache() {
      return cache;
   }

   public void listHistory(CommandSender sender, int pageAdjust) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      final String temp = cache.get(sender.getName());
      String name = logic.getPlayerInDatabase(temp);
      if(name == null) {
         name = temp;
      }
      final List<String> list = database.getPlayerHistory(name);
      if(list.isEmpty()) {
         sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " No history for " + ChatColor.AQUA + name);
         cache.remove(sender.getName());
         return;
      }
      if(!page.containsKey(sender.getName())) {
         page.put(sender.getName(), 0);
      } else {
         if(pageAdjust != 0) {
            int adj = page.get(sender.getName()).intValue() + pageAdjust;
            page.put(sender.getName(), adj);
         }
      }
      final String[] array = list.toArray(new String[0]);
      boolean valid = true;
      // Caluclate amount of pages
      int num = array.length / 8;
      double rem = (double) array.length % (double) config.limit;
      if(rem != 0) {
         num++;
      }
      if(page.get(sender.getName()).intValue() < 0) {
         // They tried to use /ks prev when they're on page 0
         sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Page does not exist");
         // reset their current page back to 0
         page.put(sender.getName(), 0);
         valid = false;
      } else if((page.get(sender.getName()).intValue()) * config.limit > array.length) {
         // They tried to use /ks next at the end of the list
         sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Page does not exist");
         // Revert to last page
         page.put(sender.getName(), num - 1);
         valid = false;
      }
      if(valid) {
         // Header with amount of pages
         sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.AQUA + name + ChatColor.BLUE + "===" + ChatColor.GRAY + "Page: "
               + ((page.get(sender.getName()).intValue()) + 1) + ChatColor.BLUE + " of " + ChatColor.GRAY + num + ChatColor.BLUE + "===");
         // list
         for(int i = ((page.get(sender.getName()).intValue()) * config.limit); i < ((page.get(sender.getName()).intValue()) * config.limit)
               + config.limit; i++) {
            // Don't try to pull something beyond the bounds
            if(i < array.length) {

               sender.sendMessage(ChatColor.translateAlternateColorCodes('&', array[i]));
            } else {
               break;
            }
         }
      }
   }

   private class HelpCommand implements JailCommand {

      @Override
      public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
         return noArgs(sender, command, label);
      }

   }

}
