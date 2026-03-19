package com.aurora.wizhistory;

public class HistoryEntry {

    private final String commandName;

    private final long timestamp;

    public HistoryEntry(String commandName, long timestamp) {
        this.commandName = commandName;
        this.timestamp = timestamp;
    }

    public String getCommandName() {
        return commandName;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
