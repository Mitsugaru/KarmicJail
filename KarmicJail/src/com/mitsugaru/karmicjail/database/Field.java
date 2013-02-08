package com.mitsugaru.karmicjail.database;

/**
 * Represents all possible fields per table.
 */
public enum Field {
   PLAYERNAME(Table.JAILED, "playername", Type.STRING),
   STATUS(Table.JAILED, "status", Type.STRING),
   TIME(Table.JAILED, "time", Type.DOUBLE),
   GROUPS(Table.JAILED, "groups", Type.STRING),
   JAILER(Table.JAILED, "jailer", Type.STRING),
   DATE(Table.JAILED, "date", Type.STRING),
   REASON(Table.JAILED, "reason", Type.STRING),
   MUTE(Table.JAILED, "muted", Type.INT),
   LAST_POSITION(Table.JAILED, "lastpos", Type.STRING),
   HISTORY(Table.HISTORY, "history", Type.STRING),
   INV_SLOT(Table.INVENTORY, "slot", Type.INT),
   INV_ITEM(Table.INVENTORY, "itemid", Type.INT),
   INV_AMOUNT(Table.INVENTORY, "amount", Type.INT),
   INV_DATA(Table.INVENTORY, "data", Type.STRING),
   INV_DURABILITY(Table.INVENTORY, "durability", Type.STRING),
   INV_ENCHANT(Table.INVENTORY, "enchantments", Type.STRING);
   private final Table table;
   private final Type type;
   private final String columnname;

   private Field(Table table, String column, Type type) {
      this.table = table;
      this.columnname = column;
      this.type = type;
   }

   public Table getTable() {
      return table;
   }

   public String getColumnName() {
      return columnname;
   }

   public Type getType() {
      return type;
   }
}
