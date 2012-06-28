package com.mitsugaru.karmicjail.commands;

public class ComparableCommand implements Comparable<ComparableCommand> {

    private String command;

    public ComparableCommand(String command) {
	this.command = command;
    }

    public int length() {
	return this.command.length();
    }

    public String getCommand() {
	return command;
    }

    public String[] split() {
	return command.split(" ");
    }

    private boolean partialCompare(String a, String b) {
	if (a.equals("*") || (b.equals("*"))) {
	    return true;
	} else if (a.equalsIgnoreCase(b)) {
	    return true;
	}
	return false;
    }

    @Override
    public int hashCode() {
	return command.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof ComparableCommand) {
	    return this.command.equals(((ComparableCommand) obj).getCommand());
	}
	return false;
    }

    @Override
    public int compareTo(ComparableCommand com) {
	int diff = 0;
	final String[] com1Array = this.split();
	final String[] com2Array = com.split();
	if (com1Array.length <= com2Array.length) {
	    for (int i = 0; i < com1Array.length; i++) {
		if (!partialCompare(com1Array[i], com2Array[i])) {
		    diff += 1;
		}
	    }
	} else if (com1Array.length > com2Array.length) {
	    for (int i = 0; i < com2Array.length; i++) {
		if (!partialCompare(com1Array[i], com2Array[i])) {
		    diff -= 1;
		}
	    }
	}
	return diff;
    }
}