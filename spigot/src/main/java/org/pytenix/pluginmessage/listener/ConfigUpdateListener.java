package org.pytenix.pluginmessage.listener;

import org.pytenix.SpigotTranslator;
import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.pluginmessage.SpigotTransport;

import java.io.IOException;

public class ConfigUpdateListener {

    final SpigotTranslator spigotTranslator;
    final SpigotTransport spigotTransport;

    public ConfigUpdateListener(SpigotTranslator spigotTranslator) {
        this.spigotTranslator = spigotTranslator;
        this.spigotTransport = spigotTranslator.getSpigotTransport();
    }

    @OmniSubscribe(priority = 90)
    public void onConfigUpdate(ConfigUpdateEvent event) {


        spigotTranslator.getTaskScheduler().runAsync(() -> {


            try {
                if (!spigotTranslator.getDataFolder().exists()) spigotTranslator.getDataFolder().mkdirs();
                spigotTranslator.getMapper().writeValue(spigotTranslator.getConfigFile(), event.translationConfiguration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        spigotTranslator.getLogger().info("Config-Update vom Proxy empfangen und angewendet.");
    }
}
