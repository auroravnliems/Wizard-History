package com.aurora.wizhistory;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class WizHistoryExpansion extends PlaceholderExpansion {

    private final WizardHistory plugin;

    public WizHistoryExpansion(WizardHistory plugin) {
        this.plugin = plugin;
    }


    @Override
    public @NotNull String getIdentifier() {
        return "wizhistory";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Aurora_VN";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }


    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        List<HistoryEntry> history = plugin.getHistoryManager().getHistory(uuid);
        params = params.toLowerCase();

        if (params.equals("alltime_total")) {
            return String.valueOf(plugin.getHistoryManager().getAlltimeTotal(uuid));
        }

        if (params.startsWith("alltime_")) {
            String cmd = params.substring(8);
            return String.valueOf(plugin.getHistoryManager().getAlltimeTotal(uuid, cmd));
        }

        if (params.equals("total")) {
            return String.valueOf(history.size());
        }

        if (params.startsWith("last_")) {
            if (history.isEmpty()) return "";
            HistoryEntry first = history.get(0);
            return switch (params.substring(5)) {
                case "name" -> getDisplayName(first.getCommandName());
                case "raw"  -> first.getCommandName();
                case "time" -> formatTime(first.getTimestamp());
                default     -> null;
            };
        }

        if (params.startsWith("total_")) {
            String cmd = params.substring(6);
            long count = history.stream()
                    .filter(e -> e.getCommandName().equals(cmd))
                    .count();
            return String.valueOf(count);
        }

        if (params.startsWith("pity_")) {
            String targetCmd = params.substring(5);
            int pity = 0;
            for (HistoryEntry e : history) {
                if (e.getCommandName().equals(targetCmd)) break;
                pity++;
            }
            return String.valueOf(pity);
        }

        if (params.startsWith("entry_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3) return null;
            int index;
            try {
                index = Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException e) {
                return null;
            }
            if (index < 0 || index >= history.size()) return "";
            HistoryEntry entry = history.get(index);
            return resolveField(entry, parts[2]);
        }

        if (params.startsWith("maxpage_")) {
            int perPage;
            try { perPage = Integer.parseInt(params.substring(8)); }
            catch (NumberFormatException e) { return null; }
            if (perPage <= 0) return "0";
            int total = history.size();
            return String.valueOf(total == 0 ? 1 : (int) Math.ceil((double) total / perPage));
        }

        if (params.startsWith("page_")) {
            String sub = params.substring(5);
            String[] parts = sub.split("_", 4);
            if (parts.length < 4) return null;
            int page, perPage, slot;
            try {
                page    = Integer.parseInt(parts[0]);
                perPage = Integer.parseInt(parts[1]);
                slot    = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            String field = parts[3];
            int globalIndex = (page - 1) * perPage + (slot - 1);
            if (globalIndex < 0 || globalIndex >= history.size()) return "";
            return resolveField(history.get(globalIndex), field);
        }

        return null;
    }


    private @Nullable String resolveField(HistoryEntry entry, String field) {
        return switch (field) {
            case "name"      -> getDisplayName(entry.getCommandName());
            case "raw"       -> entry.getCommandName();
            case "time"      -> formatTime(entry.getTimestamp());
            case "timestamp" -> String.valueOf(entry.getTimestamp());
            default          -> null;
        };
    }

    private String getDisplayName(String commandName) {
        String lower = commandName.toLowerCase();

        String fromFile = plugin.getCommandFileReader().getDisplayName(lower);
        if (fromFile != null) return fromFile;

        String fromConfig = plugin.getConfig().getString("display-names." + lower);
        if (fromConfig != null) return fromConfig;

        return commandName;
    }

    private String formatTime(long timestamp) {
        String pattern = plugin.getConfig().getString("date-format", "yyyy/MM/dd HH:mm");
        try {
            return new SimpleDateFormat(pattern).format(new Date(timestamp));
        } catch (IllegalArgumentException e) {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(timestamp));
        }
    }
}
