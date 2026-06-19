package org.pytenix.network.listener;

import org.pytenix.TranslatorPlugin;
import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.network.SpigotTransport;

import java.io.IOException;

public class ConfigUpdateListener {

    final TranslatorPlugin translatorPlugin;
    final SpigotTransport spigotTransport;

    public ConfigUpdateListener(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
        this.spigotTransport = translatorPlugin.getSpigotTransport();
    }

    @OmniSubscribe(priority = 90)
    public void onConfigUpdate(ConfigUpdateEvent event) {


        translatorPlugin.getTaskScheduler().runAsync(() -> {


            try {
                if (!translatorPlugin.getDataFolder().exists()) translatorPlugin.getDataFolder().mkdirs();
                translatorPlugin.getMapper().writeValue(translatorPlugin.getConfigFile(), event.translationConfiguration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        translatorPlugin.getLogger().info("Config-Update vom Proxy empfangen und angewendet.");
    }
}
