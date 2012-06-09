package com.mitsugaru.karmicjail.database;

import com.mitsugaru.karmicjail.config.RootConfig;

public enum Table
{
	JAILED(RootConfig.tablePrefix + "jailed"), INVENTORY(RootConfig.tablePrefix
			+ "inventory"), HISTORY(RootConfig.tablePrefix + "history");
	private final String table;

	private Table(String table)
	{
		this.table = table;
	}

	public String getName()
	{
		return table;
	}
}
