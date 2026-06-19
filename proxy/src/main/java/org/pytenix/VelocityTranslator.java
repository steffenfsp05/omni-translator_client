package org.pytenix;


import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.pytenix.backend.GeoService;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.backend.RestfulService;
import org.pytenix.cache.CacheProvider;
import org.pytenix.cache.impl.CaffeineCacheProvider;
import org.pytenix.config.ConfigService;
import org.pytenix.config.ConfigurationFile;
import org.pytenix.listener.PlayerConnectionChangeListener;
import org.pytenix.listener.ProxyPingListener;
import org.pytenix.pluginmessage.ProxyTransport;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.impl.DefaultTranslationService;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Plugin(
        id = "translator",
        name = "TranslatorProxy",
        version = "1.0-SNAPSHOT",
        authors = {"PytenixOG"}
)
public class VelocityTranslator {

    final ConfigService configService;
    final ConfigurationFile configurationFile;
    private final ProxyServer server;
    private final Logger logger;
    @Getter
    private final CacheProvider<String, String> caffeineCache;
    @Getter
    private final ProxyServer proxyServer;
    @Getter
    OmniConnectionService connectionService;
    @Getter
    GeoService geoService;
    @Getter
    String remoteAddress = "192.168.178.121:8083";
    @Getter
    TranslatorService translatorService;
    @Getter
    ProxyTransport proxyTransport;
    @Getter
    private RestfulService restfulService;

    @Inject
    public VelocityTranslator(ProxyServer server, Logger logger) {
        this.server = server;
        this.proxyServer = server;
        this.logger = logger;
        this.caffeineCache = new CaffeineCacheProvider();


        this.configService = new ConfigService();

        if (!configService.exists("config.json")) {
            configService.saveConfig("config.json", new ConfigurationFile("DEIN-LIZENZ-SCHLÜSSEL"));
            System.out.println("[AITranslator] Please check in config.json for the license key!");
        }
        this.configurationFile = configService.loadConfig("config.json", ConfigurationFile.class);


    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {


        this.translatorService = new DefaultTranslationService((id, text, targetLang, module) ->
                restfulService.sendTranslationRequest(id, text, targetLang, module));

        final String secret = loadForwardingSecret();

        if (secret == null || secret.isEmpty()) {
            logger.error(" ");
            logger.error("====================================================");
            logger.error("OMNIPROXY INITIALISIERUNG FEHLGESCHLAGEN!");
            logger.error("Die Datei 'forwarding.secret' wurde nicht gefunden.");
            logger.error("Stelle sicher, dass 'player-forwarding-mode = \"modern\"' in der velocity.toml aktiv ist.");
            logger.error("Das Plugin wird jetzt DEAKTIVIERT.");
            logger.error("====================================================");
            logger.error(" ");

            server.getEventManager().unregisterListeners(this);

            return;
        }


        this.proxyTransport = new ProxyTransport(this, "ABC");


        this.connectionService = new OmniConnectionService(
                this,
                configurationFile.getLicenseKey(),
                proxyServer
        );

        this.restfulService = new RestfulService(
                this,
                connectionService
        );
        this.geoService = new GeoService(this, proxyServer, connectionService);

        connectionService.setServices(restfulService, geoService);
        connectionService.connect();


        server.getEventManager().register(this, new ProxyPingListener(this));
        server.getEventManager().register(this, new PlayerConnectionChangeListener(this));

        logger.info("Translator Proxy erfolgreich gestartet!");
    }


    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        proxyTransport.shutdown();
    }

    private String loadForwardingSecret() {
        Path secretPath = Paths.get("forwarding.secret");

        if (!Files.exists(secretPath)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(secretPath);
            if (!lines.isEmpty()) {
                return lines.get(0).trim();
            }
        } catch (IOException e) {
            logger.error("Fehler beim Lesen der forwarding.secret Datei:", e);
        }
        return null;
    }


}