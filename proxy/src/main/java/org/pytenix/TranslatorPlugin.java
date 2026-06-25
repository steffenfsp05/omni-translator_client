package org.pytenix;


import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.pytenix.backend.GeoSocketEndpoint;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.backend.TranslationSocketEndpoint;
import org.pytenix.cache.CacheProvider;
import org.pytenix.cache.impl.CaffeineCacheProvider;
import org.pytenix.chat.MessageSequencer;
import org.pytenix.chat.SystemChatModule;
import org.pytenix.config.ConfigService;
import org.pytenix.config.ConfigurationFile;
import org.pytenix.event.EventService;
import org.pytenix.event.impl.DefaultEventService;
import org.pytenix.limbo.LimboService;
import org.pytenix.listener.PlayerConnectionChangeListener;
import org.pytenix.listener.ProxyPingListener;
import org.pytenix.network.ProxyTransport;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.placeholder.GradientService;
import org.pytenix.placeholder.PlaceholderService;
import org.pytenix.placeholder.impl.DefaultGradientService;
import org.pytenix.placeholder.impl.DefaultPlaceholderService;
import org.pytenix.profile.ProfileService;
import org.pytenix.profile.impl.DefaultProfileService;
import org.pytenix.translation.TranslationProcessor;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.impl.DefaultTranslationService;
import org.pytenix.util.TextComponentUtil;
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
@Getter
public class TranslatorPlugin {

    final ConfigService configService;
    final ConfigurationFile configurationFile;

    final ProxyServer server;
    final Logger logger;
    final CacheProvider<String, String> caffeineCache;
    final ProxyServer proxyServer;

    TranslationSocketEndpoint translationSocketEndpoint;
    OmniConnectionService connectionService;
    GeoSocketEndpoint geoSocketEndpoint;

    String remoteAddress = "192.168.178.121:8083";

    ProxyTransport proxyTransport;


    TranslatorService translatorService;
    TranslationProcessor translationProcessor;
    PlaceholderService placeholderService;
    GradientService gradientService;
    EventService eventService;
    ProfileService profileService;

    SystemChatModule systemChatService;
    TextComponentUtil textComponentUtil;
    MessageSequencer messageSequencer;

    LimboService limboService;

    @Inject
    public TranslatorPlugin(ProxyServer server, Logger logger) {
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


        this.translationProcessor = (id, text, targetLang, module) -> translationSocketEndpoint.sendTranslationRequest(id, text, targetLang, module);
        this.placeholderService = new DefaultPlaceholderService();
        this.gradientService = new DefaultGradientService();
        this.eventService = new DefaultEventService();


        this.translatorService = new DefaultTranslationService(translationProcessor, placeholderService, gradientService, eventService);


        this.textComponentUtil = new TextComponentUtil(translatorService);
        this.messageSequencer = new MessageSequencer(this, textComponentUtil);


        this.systemChatService = new SystemChatModule(
                this,
                translatorService,
                textComponentUtil,
                messageSequencer,
                uuid -> this.getProxyServer().getPlayer(uuid).get().getEffectiveLocale().toString().toLowerCase()
        );

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


        this.limboService = new LimboService(this, proxyServer, 25588, secret);


        System.out.println("READING SECRET: " + secret);

        this.proxyTransport = new ProxyTransport(this, secret);


        this.connectionService = new OmniConnectionService(
                this,
                configurationFile.getLicenseKey(),
                proxyServer
        );

        this.translationSocketEndpoint = new TranslationSocketEndpoint(
                this,
                connectionService
        );

        this.geoSocketEndpoint = new GeoSocketEndpoint(connectionService);

        this.profileService = new DefaultProfileService(
                configurationFile::getLicenseKey,
                profilePacket -> connectionService.sendPacket(PacketRegistry.PROFILE, profilePacket)
        );

        connectionService.setServices(translationSocketEndpoint, geoSocketEndpoint, profileService);
        connectionService.connect();



        server.getEventManager().register(this, new ProxyPingListener(this));
        server.getEventManager().register(this, new PlayerConnectionChangeListener(this));


        logger.info("Translator Proxy erfolgreich gestartet!");
    }


    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        proxyTransport.shutdown();
        connectionService.shutdown();
        limboService.shutdown();
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