package com.imjake9.simplejail;

public class JailTask implements Runnable {
	private SimpleJail sj;
	private long start = System.nanoTime();
	private long duration;
	private String name;
	private int id;

	public JailTask()
	{
		duration = 0;
	}

	public JailTask(SimpleJail plugin, String playerName, long time)
	{
		sj = plugin;
		name = playerName;
		duration = time;
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

	public void stop()
	{
		if(id != -1)
		{
			//Stop thread
			sj.getServer().getScheduler().cancelTask(id);
		}
		long early = System.nanoTime() - start;
		duration -= early;
		sj.updatePlayerTime(name, duration);
		sj.removeTask(name);
	}

	public int getId() {
		return id;
	}
}
