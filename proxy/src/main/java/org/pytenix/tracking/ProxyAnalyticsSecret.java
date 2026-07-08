package org.pytenix.tracking;

import org.pytenix.profile.AbstractAnalyticsSecret;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ProxyAnalyticsSecret extends AbstractAnalyticsSecret {

    private final Logger logger;
    private final Path configFile;

    public ProxyAnalyticsSecret(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.configFile = dataDirectory.resolve("analytics.properties");
    }

    @Override
    protected String getStoredSalt() {
        if (!Files.exists(configFile)) {
            return null;
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(configFile.toFile())) {
            props.load(in);
            return props.getProperty("secret_salt");
        } catch (IOException e) {
            logError("Fehler beim Lesen des Analytics-Salts!", e);
            return null;
        }
    }

    @Override
    protected void saveSaltToConfig(String salt) {
        Properties props = new Properties();
        props.setProperty("secret_salt", salt);

        try (FileOutputStream out = new FileOutputStream(configFile.toFile())) {
            props.store(out, "MALS NIEMALS DIESEN SALT ÄNDERN ODER LÖSCHEN!\nWenn der Salt gelöscht wird, ändern sich alle IDs im Backend und deine historischen ROI-Daten sind wertlos.");
        } catch (IOException e) {
            logError("Fehler beim Speichern des Analytics-Salts!", e);
        }
    }

    @Override
    protected void logInfo(String message) {
        logger.info(message);
    }

    @Override
    protected void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}