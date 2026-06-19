package org.pytenix.network;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class VelocitySecretReader {

    public String loadVelocitySecret() {
        File configFile = new File("config/paper-global.yml");

        if (!configFile.exists()) {
            configFile = new File("paper.yml");
        }

        if (!configFile.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            String secret = config.getString("proxies.velocity.secret");

            if (secret == null) {
                secret = config.getString("settings.velocity-support.secret");
            }

            return secret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}