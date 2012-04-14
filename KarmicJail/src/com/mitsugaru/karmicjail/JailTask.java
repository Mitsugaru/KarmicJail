package com.mitsugaru.karmicjail;

public class JailTask implements Runnable {
	private KarmicJail plugin;
	private long start;
	private long duration;
	private String name;
	private int id;

	public JailTask()
	{
		duration = 0;
	}

	public JailTask(KarmicJail plugin, String playerName, long time)
	{
		this.plugin = plugin;
		name = playerName;
		duration = (long) Math.floor(time + 0.5f);
		start = System.nanoTime();
		id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, duration);
	}

	@Override
	public void run() {
		JailLogic.unjailPlayer(plugin.getServer().getConsoleSender(), name, true);
	}

	public String getName()
	{
		return name;
	}

	public long remainingTime()
	{
		return (duration - (long) (Math.floor(((System.nanoTime() - start) * 0.000000001) + 0.5f)) * 20);
	}

	public boolean stop()
	{
		if(id != -1)
		{
			//Stop thread
			plugin.getServer().getScheduler().cancelTask(id);
			final long early = System.nanoTime() - start;
			duration -= (long) (Math.floor((early * 0.000000001) + 0.5f) * 20);
			JailLogic.updatePlayerTime(name, duration);
			KarmicJail.removeTask(name);
			return true;
		}
		return false;
		
	}

	public int getId() {
		return id;
	}
}
