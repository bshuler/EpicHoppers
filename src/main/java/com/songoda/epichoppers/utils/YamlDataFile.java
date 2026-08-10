package com.songoda.epichoppers.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Minimal local replacement for Arconix's {@code ConfigWrapper}: loads (or
 * creates, if absent) a YAML file in the plugin's data folder and exposes it
 * as a standard Bukkit {@link FileConfiguration}. See CLAUDE.md for why
 * Arconix was removed.
 */
public class YamlDataFile {

    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration config;

    public YamlDataFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create " + fileName + ": " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
