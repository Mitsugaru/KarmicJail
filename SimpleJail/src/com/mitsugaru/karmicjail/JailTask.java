package com.mitsugaru.karmicjail;

public class JailTask implements Runnable {
	private KarmicJail sj;
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
		sj = plugin;
		name = playerName;
		duration = (long) Math.floor(time + 0.5f);
		start = System.nanoTime();
		id = sj.getServer().getScheduler().scheduleSyncDelayedTask(sj, this, duration);
	}

	@Override
	public void run() {
		final String[] args = {name};
		sj.unjailPlayer(sj.console, args);
	}

	public String getName()
	{
		return name;
	}

	public long remainingTime()
	{
		return (duration - (long) Math.floor(((System.nanoTime() - start) * 0.000000001) + 0.5f));
	}

	public void stop()
	{
		if(id != -1)
		{
			//Stop thread
			sj.getServer().getScheduler().cancelTask(id);
		}
		long early = System.nanoTime() - start;
		duration -= (long) Math.floor((early * 0.000000001) + 0.5f);
		sj.updatePlayerTime(name, duration);
		sj.removeTask(name);
	}

	public int getId() {
		return id;
	}
}
