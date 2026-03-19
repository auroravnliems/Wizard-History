package com.aurora.wizhistory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class WizardHistory extends JavaPlugin {

    private static WizardHistory instance;

    private HistoryManager historyManager;
    private CommandFileReader commandFileReader;


    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        historyManager = new HistoryManager(this);

        commandFileReader = new CommandFileReader(this);

        Bukkit.getPluginManager().registerEvents(new CommandTracker(this), this);

        AdminCommand adminCmd = new AdminCommand(this);
        Objects.requireNonNull(getCommand("wizhistory")).setExecutor(adminCmd);
        Objects.requireNonNull(getCommand("wizhistory")).setTabCompleter(adminCmd);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WizHistoryExpansion(this).register();
            getLogger().info("PlaceholderAPI found – placeholders registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will NOT work.");
        }

        if (Bukkit.getPluginManager().getPlugin("WizardCommand") == null) {
            getLogger().warning("WizardCommand is not loaded. History tracking will not work.");
        }

        getLogger().info("WizardHistory v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (historyManager != null) {
            historyManager.saveAll();
        }
        getLogger().info("WizardHistory disabled – all history saved.");
    }


    public static WizardHistory getInstance() {
        return instance;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public CommandFileReader getCommandFileReader() {
        return commandFileReader;
    }
}
