package org.pytenix.injection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.omni.config.ConfigService;
import org.omni.config.ConfigurationFile;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.player.LiveChatModule;
import org.slf4j.Logger;

import java.io.File;

public class TranslatorSpigotModule extends AbstractModule {

    private final TranslatorPlugin plugin;

    public TranslatorSpigotModule(TranslatorPlugin plugin) {
        this.plugin = plugin;
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
    @Named("configFile")
    public File provideProxySyncConfigFile() {
        return new File(plugin.getDataFolder(), "proxy_sync_config.json");
    }


}
