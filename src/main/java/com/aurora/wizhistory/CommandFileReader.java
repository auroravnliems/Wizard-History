package com.aurora.wizhistory;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CommandFileReader {

    private final Map<String, String> displayNameCache = new HashMap<>();

    private final File wizardCommandFolder;
    private final Logger logger;

    public CommandFileReader(WizardHistory plugin) {
        this.logger = plugin.getLogger();
        this.wizardCommandFolder = new File(
                plugin.getDataFolder()
                        .getParentFile()
                , "WizardCommand/commands"
        );
        loadAll();
    }

    public String getDisplayName(String commandName) {
        return displayNameCache.get(commandName.toLowerCase());
    }

    public void reload() {
        displayNameCache.clear();
        loadAll();
    }


    private void loadAll() {
        if (!wizardCommandFolder.exists() || !wizardCommandFolder.isDirectory()) {
            logger.warning("WizardCommand/commands folder not found at: "
                    + wizardCommandFolder.getAbsolutePath()
                    + " – display-name reading from command files is disabled.");
            return;
        }

        File[] files = wizardCommandFolder.listFiles(
                (dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml")
        );

        if (files == null || files.length == 0) return;

        int loaded = 0;
        for (File file : files) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
                if (!yml.contains("display-name")) continue;

                String displayName = yml.getString("display-name");
                if (displayName == null || displayName.isBlank()) continue;

                String key = stripExtension(file.getName()).toLowerCase();
                displayNameCache.put(key, displayName);
                loaded++;

            } catch (Exception e) {
                logger.warning("Failed to read display-name from " + file.getName() + ": " + e.getMessage());
            }
        }

        if (loaded > 0) {
            logger.info("Loaded display-name from " + loaded + " WizardCommand file(s).");
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
