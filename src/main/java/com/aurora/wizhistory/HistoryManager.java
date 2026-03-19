package com.aurora.wizhistory;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HistoryManager {

    private final WizardHistory plugin;

    private final Map<UUID, List<HistoryEntry>> cache = new HashMap<>();

    private final Map<UUID, Map<String, Long>> alltimeCache = new HashMap<>();

    private final File dataFolder;

    public HistoryManager(WizardHistory plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
        }
    }

    public void addEntry(Player player, String commandName) {
        List<String> trackOnly = plugin.getConfig().getStringList("track-only");
        List<String> ignore    = plugin.getConfig().getStringList("ignore-commands");

        String lowerCmd = commandName.toLowerCase();

        if (!ignore.isEmpty() && ignore.stream().anyMatch(s -> s.equalsIgnoreCase(lowerCmd))) return;
        if (!trackOnly.isEmpty() && trackOnly.stream().noneMatch(s -> s.equalsIgnoreCase(lowerCmd))) return;

        UUID uuid = player.getUniqueId();

        List<HistoryEntry> list = getOrLoadHistory(uuid);
        list.addFirst(new HistoryEntry(lowerCmd, System.currentTimeMillis()));

        int max = plugin.getConfig().getInt("max-history", 200);
        if (max > 0) {
            while (list.size() > max) list.removeLast();
        }

        Map<String, Long> alltime = getOrLoadAlltime(uuid);
        alltime.merge(lowerCmd, 1L, Long::sum);

        saveHistory(uuid, list, alltime);
    }

    public List<HistoryEntry> getHistory(UUID uuid) {
        return Collections.unmodifiableList(getOrLoadHistory(uuid));
    }

    public long getAlltimeTotal(UUID uuid) {
        return getOrLoadAlltime(uuid).values().stream().mapToLong(Long::longValue).sum();
    }

    public long getAlltimeTotal(UUID uuid, String commandName) {
        return getOrLoadAlltime(uuid).getOrDefault(commandName.toLowerCase(), 0L);
    }

    public void clearHistory(UUID uuid) {
        cache.put(uuid, new ArrayList<>());
        alltimeCache.put(uuid, new LinkedHashMap<>());
        File file = fileFor(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Failed to delete history file for " + uuid);
        }
    }

    public void clearRollingHistory(UUID uuid) {
        List<HistoryEntry> emptyList = new ArrayList<>();
        cache.put(uuid, emptyList);
        Map<String, Long> alltime = getOrLoadAlltime(uuid);
        saveHistory(uuid, emptyList, alltime);
    }

    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
        alltimeCache.remove(uuid);
    }

    public void saveAll() {
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(cache.keySet());
        allUuids.addAll(alltimeCache.keySet());
        for (UUID uuid : allUuids) {
            saveHistory(uuid, getOrLoadHistory(uuid), getOrLoadAlltime(uuid));
        }
    }

    private List<HistoryEntry> getOrLoadHistory(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadHistory);
    }

    private Map<String, Long> getOrLoadAlltime(UUID uuid) {
        return alltimeCache.computeIfAbsent(uuid, this::loadAlltime);
    }

    private List<HistoryEntry> loadHistory(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return new ArrayList<>();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        List<?> raw = yml.getList("history", new ArrayList<>());
        List<HistoryEntry> entries = new ArrayList<>();

        for (Object obj : raw) {
            if (obj instanceof Map<?, ?> map) {
                Object cmdObj = map.get("command");
                Object tsObj  = map.get("timestamp");
                String cmd = (cmdObj != null) ? String.valueOf(cmdObj) : "unknown";
                long ts;
                try {
                    ts = (tsObj != null) ? Long.parseLong(String.valueOf(tsObj)) : 0L;
                } catch (NumberFormatException e) {
                    ts = 0L;
                }
                entries.add(new HistoryEntry(cmd, ts));
            }
        }
        return entries;
    }

    private Map<String, Long> loadAlltime(UUID uuid) {
        File file = fileFor(uuid);
        Map<String, Long> result = new LinkedHashMap<>();
        if (!file.exists()) return result;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("alltime");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                result.put(key, sec.getLong(key, 0L));
            }
        }
        return result;
    }

    private void saveHistory(UUID uuid, List<HistoryEntry> entries, Map<String, Long> alltime) {
        File file = fileFor(uuid);
        YamlConfiguration yml = new YamlConfiguration();

        List<Map<String, Object>> raw = new ArrayList<>();
        for (HistoryEntry e : entries) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("command", e.getCommandName());
            map.put("timestamp", e.getTimestamp());
            raw.add(map);
        }
        yml.set("history", raw);

        for (Map.Entry<String, Long> e : alltime.entrySet()) {
            yml.set("alltime." + e.getKey(), e.getValue());
        }

        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save history for " + uuid + ": " + ex.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }
}
