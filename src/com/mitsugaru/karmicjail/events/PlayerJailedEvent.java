package com.mitsugaru.karmicjail.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mitsugaru.karmicjail.jail.PrisonerInfo;

/**
 * Event called when player is jailed.
 */
public class PlayerJailedEvent extends Event {
   /**
    * Handler list.
    */
   private static final HandlerList handlers = new HandlerList();
   /**
    * Name of player jailed.
    */
   private final String name;
   /**
    * Date that it occurred.
    */
   private final String date;
   /**
    * Player that issued the jail order.
    */
   private final String jailer;
   /**
    * Reason the player was jailed.
    */
   private final String reason;
   /**
    * Duration of jail time.
    */
   private final long duration;

   /**
    * Constructor.
    * 
    * @param pi
    *           - PrisonerInfo.
    */
   public PlayerJailedEvent(PrisonerInfo pi) {
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

   public static HandlerList getHandlerList() {
      return handlers;
   }

}
