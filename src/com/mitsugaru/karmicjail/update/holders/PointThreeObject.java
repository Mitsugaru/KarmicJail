package com.mitsugaru.karmicjail.update.holders;

public class PointThreeObject
{
	public String playername, status, groups, jailer, date, reason;
	public double time;
	public int mute;

	public PointThreeObject(String name, String status, String groups,
			String jailer, String date, String reason, double time, int mute)
	{
		this.playername = name;
		this.status = status;
		this.groups = groups;
		this.jailer = jailer;
		this.date = date;
		this.reason = reason;
		this.time = time;
		this.mute = mute;
	}
}
