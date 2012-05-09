package com.mitsugaru.karmicjail.permissions;

public enum PermissionNode
{
	JAIL(".jail"), TIMED(".timed"), UNJAIL(".unjail"), SETJAIL(".setjail"), LIST(
			".list"), MUTE(".mute"), INVENTORY_VIEW(".inventory.view"), INVENTORY_MODIFY(
			".inventory.modify"), WARP_JOINIGNORE(".warp.joinignore"), WARP_JAIL(
			".warp.jail"), WARP_LAST(".warp.last"), HISTORY_VIEW(".history.view"), HISTORY_ADD(
			".history.add"), JAILSTATUS(".jailstatus"), BROADCAST(".broadcast"), EXEMPT(
			".exempt");
	private static final String prefix = "KarmicJail";
	private String node;

	private PermissionNode(String node)
	{
		this.node = prefix + node;
	}

	public String getNode()
	{
		return node;
	}
}
