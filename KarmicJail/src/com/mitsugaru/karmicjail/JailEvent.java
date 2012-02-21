package com.mitsugaru.karmicjail;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import com.mitsugaru.karmicjail.KarmicJail.PrisonerInfo;

public class JailEvent extends Event implements Cancellable {
	private boolean cancel;
	public String name, date,jailer,reason;
	public long duration;

	//TODO fix
	public JailEvent(String event, PrisonerInfo pi)
	{
		super();
		name = pi.name;
		date = pi.date;
		jailer = pi.jailer;
		reason = pi.reason;
		duration = pi.time;
		cancel = false;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean c) {
		cancel = c;
	}

}
