package com.mitsugaru.karmicjail.modules;

import java.util.HashMap;
import java.util.Map;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.interfaces.ITaskModule;
import com.mitsugaru.karmicjail.services.AbstractModule;
import com.mitsugaru.karmicjail.services.Order;
import com.mitsugaru.karmicjail.services.Service;
import com.mitsugaru.karmicjail.tasks.JailTask;

/**
 * Handles the Runnable tasks for the plugin.
 */
@Service(order = Order.HIGHEST)
public class TaskModule extends AbstractModule implements ITaskModule {

    /**
     * Running auto-release threads for jailed players with timers.
     */
    private final Map<String, JailTask> threads = new HashMap<String, JailTask>();

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin reference.
     */
    public TaskModule(KarmicJail plugin) {
        super(plugin);
    }

    @Override
    public Map<String, JailTask> getJailTasks() {
        return threads;
    }

    @Override
    public void addTask(String name, long time) {
        threads.put(name, new JailTask(plugin, name, time));
    }

    @Override
    public void removeTask(String name) {
        threads.remove(name);
    }

    @Override
    public boolean stopTask(String name) {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(threads.containsKey(name)) {
            if(config.debugLog && config.debugEvents) {
                plugin.getLogger().info("Thread found for: " + name);
            }
            final boolean stop = threads.get(name).stop();
            if(config.debugLog && config.debugEvents) {
                if(stop) {
                    plugin.getLogger().info("Thread stopped for: " + name);
                } else {
                    plugin.getLogger().warning(
                            "Thread NOT stopped for: " + name);
                }
            }
            return stop;
        } else {
            if(config.debugLog && config.debugEvents) {
                plugin.getLogger().warning("Thread NOT found for: " + name);
            }
        }
        return false;
    }

    @Override
    public void starting() {
    }

    @Override
    public void closing() {
        plugin.getLogger().info("Stopping all jail threads...");
        for(JailTask task : threads.values()) {
            task.stop();
        }
    }
}
