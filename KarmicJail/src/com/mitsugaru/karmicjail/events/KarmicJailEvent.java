package com.mitsugaru.karmicjail.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mitsugaru.karmicjail.KarmicJail.PrisonerInfo;

public class KarmicJailEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String name, date,jailer,reason;
	private long duration;

	public KarmicJailEvent(String event, PrisonerInfo pi)
	{
		super();
		name = pi.name;
		date = pi.date;
		jailer = pi.jailer;
		reason = pi.reason;
		duration = pi.time;
	}

	public String getName() {
		return name;
	}

	public String getDate() {
		return date;
	}

	public String getJailer() {
		return jailer;
	}

	public String getReason() {
		return reason;
	}

	public long getDuration() {
		return duration;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}

}
