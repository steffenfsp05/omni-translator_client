package org.pytenix.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.pytenix.profile.AbstractAnalyticsSecret;

import java.util.logging.Level;

public class SpigotAnalyticsSecret extends AbstractAnalyticsSecret {

    private final JavaPlugin plugin;

    public SpigotAnalyticsSecret(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected String getStoredSalt() {
        return plugin.getConfig().getString("analytics.secret_salt");
    }

    @Override
    protected void saveSaltToConfig(String salt) {
        FileConfiguration config = plugin.getConfig();
        config.set("analytics.secret_salt", salt);
        plugin.saveConfig();
    }

    @Override
    protected void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    protected void logError(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}
