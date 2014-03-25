package com.mitsugaru.karmicjail.interfaces;

import java.util.Map;

import com.mitsugaru.karmicjail.services.Module;
import com.mitsugaru.karmicjail.tasks.JailTask;

/**
 * Interface for task handling module.
 */
public interface ITaskModule extends Module {

    /**
     * Get the currently running scheduled tasks for
     * 
     * @return Map of jailed online players to their associated tasks.
     */
    Map<String, JailTask> getJailTasks();

    /**
     * Creates a new thread for a player to get auto-released
     * 
     * @param name
     *            Player name.
     * @param time
     *            Duration of online jail time.
     */
    void addTask(String name, long time);

    /**
     * Removes a task. Used for when a player has logged off.
     * 
     * @param name
     *            - Name of player
     */
    void removeTask(String name);
    
    /**
     * Stops a player's timed task
     * 
     * @param name
     *            of player
     * @return true if the player's task was stopped. If unsuccessful or if
     *         player did not have a timed task, then it returns false.
     */
    boolean stopTask(String name);
}
