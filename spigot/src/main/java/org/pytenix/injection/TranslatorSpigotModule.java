package org.pytenix.injection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.omni.config.ConfigService;
import org.omni.config.ConfigurationFile;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.player.LiveChatModule;
import org.pytenix.service.SpigotAnalyticsSecret;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class TranslatorSpigotModule extends AbstractModule {

    private final TranslatorPlugin plugin;
    private final Path dataDirectory;

    public TranslatorSpigotModule(TranslatorPlugin plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(org.slf4j.LoggerFactory.getLogger("Translator"));

        bind(TranslatorPlugin.class).toInstance(plugin);
        bind(Plugin.class).toInstance(plugin);

        bind(ObjectMapper.class).in(Scopes.SINGLETON);







        Multibinder<AbstractTranslatorModule> moduleBinder = Multibinder.newSetBinder(binder(), AbstractTranslatorModule.class);
        moduleBinder.addBinding().to(InventoryModule.class);
        moduleBinder.addBinding().to(LiveChatModule.class);
        moduleBinder.addBinding().to(HologramModule.class);


    }

    @Provides
    @Singleton
    public ConfigurationFile provideConfigurationFile(ConfigService configService) {
        if (!configService.exists("config.json")) {
            configService.saveConfig("config.json", new ConfigurationFile("DEIN-LIZENZ-SCHLÜSSEL"));
            plugin.getLogger().info("[AITranslator] Please check in config.json for the license key!");
        }
        return configService.loadConfig("config.json", ConfigurationFile.class);
    }

    @Provides
    @Singleton
    public AbstractAnalyticsSecret provideAnalyticsSecret(Logger logger) {
        return new SpigotAnalyticsSecret(logger, dataDirectory);
    }

    @Provides
    @Singleton
    @Named("configFile")
    public File provideProxySyncConfigFile() {
        return new File(plugin.getDataFolder(), "proxy_sync_config.json");
    }


    @Provides
    @Singleton
    public PlayerLocaleProcessor providePlayerLocaleProcessor() {
        return uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            return (player == null) ? "en-en" : player.getLocale();
        };
    }


}
