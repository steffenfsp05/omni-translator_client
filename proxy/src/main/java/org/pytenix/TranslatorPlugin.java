package org.pytenix;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import org.omni.event.EventService;
import org.omni.injection.CoreModule;
import org.omni.translation.TranslatorService;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.backend.listener.BackendCloseListener;
import org.pytenix.backend.listener.BackendConfigUpdateListener;
import org.pytenix.backend.listener.BackendConnectListener;
import org.pytenix.backend.listener.BackendMessageReceiveListener;
import org.pytenix.chat.MessageSequencer;
import org.pytenix.chat.SystemChatModule;
import org.pytenix.chat.listener.SystemChatPacketListener;
import org.pytenix.injection.TranslatorProxyModule;
import org.pytenix.limbo.LimboService;
import org.pytenix.listener.PlayerConnectionChangeListener;
import org.pytenix.listener.ProxyPingListener;
import org.pytenix.network.ProxyTransport;
import org.pytenix.tracking.listener.PlayerConnectListener;
import org.pytenix.tracking.listener.PlayerDisconnectListener;
import org.pytenix.tracking.listener.PlayerSettingsChangeListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Plugin(
        id = "translator",
        name = "TranslatorProxy",
        version = "1.0-SNAPSHOT",
        authors = {"PytenixOG"}
)
@Getter
public class TranslatorPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final Injector velocityInjector;

    private ProxyTransport proxyTransport;
    private OmniConnectionService connectionService;
    private LimboService limboService;

    @Inject
    public TranslatorPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Injector velocityInjector) {
        this.proxyServer = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.velocityInjector = velocityInjector;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        createDataDirectory();

        final String secret = loadForwardingSecret();

        if (secret == null || secret.isEmpty()) {
            printSecretError();
            return;
        }
        System.out.println("READING SECRET: " + secret);


        Injector appInjector = velocityInjector.createChildInjector(
                new CoreModule(),
                new TranslatorProxyModule(
                        this,
                        secret,
                        dataDirectory,
                        "ws://192.168.178.121:8083/ws/omni",
                        MinecraftChannelIdentifier.from("translator:main"))
        );

        this.proxyTransport = appInjector.getInstance(ProxyTransport.class);
        this.connectionService = appInjector.getInstance(OmniConnectionService.class);
        this.limboService = appInjector.getInstance(LimboService.class);

        this.connectionService.connect();

        registerListeners(appInjector);

        logger.info("Translator Proxy erfolgreich gestartet!");
    }

    private void registerListeners(Injector injector) {

        PacketEvents.getAPI().getEventManager().registerListener(
                injector.getInstance(SystemChatPacketListener.class),
                PacketListenerPriority.HIGHEST
        );


        EventService eventService = injector.getInstance(EventService.class);

        Set<Object> velocityListeners = injector.getInstance(Key.get(new TypeLiteral<>() {
        }, Names.named("velocityListeners")));
        Set<Object> omniListeners = injector.getInstance(Key.get(new TypeLiteral<>() {
        }, Names.named("omniListeners")));


        for (Object listener : velocityListeners) {
            proxyServer.getEventManager().register(this, listener);
        }

        for (Object listener : omniListeners) {
            eventService.register(listener);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (proxyTransport != null) proxyTransport.shutdown();
        if (connectionService != null) connectionService.shutdown();
        if (limboService != null) limboService.shutdown();
    }

    private void createDataDirectory() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Konnte den Plugin-Ordner nicht erstellen!", e);
            }
        }
    }

    private String loadForwardingSecret() {
        Path secretPath = Paths.get("forwarding.secret");
        if (!Files.exists(secretPath)) return null;
        try {
            List<String> lines = Files.readAllLines(secretPath);
            return lines.isEmpty() ? null : lines.get(0).trim();
        } catch (IOException e) {
            logger.error("Fehler beim Lesen der forwarding.secret Datei:", e);
            return null;
        }
    }

    private void printSecretError() {
        logger.error(" ");
        logger.error("====================================================");
        logger.error("OMNIPROXY INITIALISIERUNG FEHLGESCHLAGEN!");
        logger.error("Die Datei 'forwarding.secret' wurde nicht gefunden.");
        logger.error("Stelle sicher, dass 'player-forwarding-mode = \"modern\"' in der velocity.toml aktiv ist.");
        logger.error("Das Plugin wird jetzt DEAKTIVIERT.");
        logger.error("====================================================");
        logger.error(" ");
        proxyServer.getEventManager().unregisterListeners(this);
    }
}