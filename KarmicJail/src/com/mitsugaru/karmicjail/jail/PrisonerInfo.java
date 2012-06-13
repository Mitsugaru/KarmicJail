package com.mitsugaru.karmicjail.jail;

public class PrisonerInfo
{
	public String name, jailer, date, reason;
	public long time;
	public boolean mute;

	public PrisonerInfo(String n, String j, String d, String r, long t,
			boolean m)
	{
		name = n;
		jailer = j;
		date = d;
		reason = r;
		time = t;
		mute = m;
	}

	public void updateTime(long t)
	{
		time = t;
	}
}
