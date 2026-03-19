package com.aurora.wizhistory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandTracker implements Listener {

    private final WizardHistory plugin;

    public CommandTracker(WizardHistory plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (!raw.startsWith("/")) return;
        raw = raw.substring(1);

        String[] parts = raw.split("\\s+", 3);
        if (parts.length < 2) return;
        if (!parts[0].equalsIgnoreCase("unioncommand")) return;

        String sub = parts[1].toLowerCase();

        if (parts.length >= 3) {
            String targetName = parts[2];
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                plugin.getHistoryManager().addEntry(target, sub);
                return;
            }
        }
        plugin.getHistoryManager().addEntry(event.getPlayer(), sub);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String raw = event.getCommand().trim();

        if (raw.startsWith("/")) raw = raw.substring(1);

        String[] parts = raw.split("\\s+", 3);
        if (parts.length < 3) return;
        if (!parts[0].equalsIgnoreCase("unioncommand")) return;

        String sub = parts[1].toLowerCase();
        String targetName = parts[2];

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return;

        plugin.getHistoryManager().addEntry(target, sub);
    }
}
