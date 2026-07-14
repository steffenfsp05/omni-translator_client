package org.pytenix.injection;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.channel.ChannelId;
import org.omni.cache.CacheProvider;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.config.ConfigService;
import org.omni.config.ConfigurationFile;
import org.omni.packets.registry.PacketRegistrar;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslationProcessor;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.ExternPacketRegistrar;
import org.pytenix.backend.endpoint.TranslationSocketEndpoint;
import org.pytenix.backend.listener.BackendCloseListener;
import org.pytenix.backend.listener.BackendConfigUpdateListener;
import org.pytenix.backend.listener.BackendConnectListener;
import org.pytenix.backend.listener.BackendMessageReceiveListener;
import org.pytenix.backend.socket.WebSocketService;
import org.pytenix.chat.listener.SystemChatPacketListener;
import org.pytenix.limbo.LimboService;
import org.pytenix.limbo.command.TranslateCommand;
import org.pytenix.limbo.listener.ServerPreConnectListener;
import org.pytenix.listener.PlayerConnectionChangeListener;
import org.pytenix.listener.ProxyPingListener;
import org.pytenix.network.InternPacketRegistrar;
import org.pytenix.network.ProxyTransport;
import org.pytenix.tracking.ExternProfileService;
import org.pytenix.tracking.ProxyAnalyticsSecret;
import org.pytenix.tracking.ROIService;
import org.pytenix.tracking.listener.PlayerConnectListener;
import org.pytenix.tracking.listener.PlayerDisconnectListener;
import org.pytenix.tracking.listener.PlayerSettingsChangeListener;
import org.slf4j.Logger;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

import java.net.http.WebSocket;
import java.nio.file.Path;

public class TranslatorProxyModule extends AbstractModule {

    private final TranslatorPlugin plugin;
    private final String forwardingSecret;
    private final Path dataDirectory;

    private final String backendRemoteAddress;
    private final ChannelIdentifier channelIdentifier;

    public TranslatorProxyModule(TranslatorPlugin plugin, String forwardingSecret, Path dataDirectory, String backendRemoteAddress, ChannelIdentifier channelIdentifier) {
        this.plugin = plugin;
        this.forwardingSecret = forwardingSecret;
        this.dataDirectory = dataDirectory;
        this.backendRemoteAddress = backendRemoteAddress;
        this.channelIdentifier = channelIdentifier;
    }

    @Override
    protected void configure() {

        bind(String.class).annotatedWith(Names.named("backendRemoteAddress")).toInstance(backendRemoteAddress);
        bind(String.class).annotatedWith(Names.named("forwardingSecret")).toInstance(forwardingSecret);

        bind(ChannelIdentifier.class).annotatedWith(Names.named("channelIdentifier")).toInstance(channelIdentifier);

        bind(new TypeLiteral<CacheProvider<String, String>>() {
        }).to(new TypeLiteral<CaffeineCacheProvider<String, String>>() {
        }).in(Scopes.SINGLETON);

        bind(ProfileService.class).to(ExternProfileService.class).in(Scopes.SINGLETON);

        bind(new TypeLiteral<PacketRegistrar<RegisteredServer>>() {
        }).to(InternPacketRegistrar.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<PacketRegistrar<WebSocket>>() {
        }).to(ExternPacketRegistrar.class).in(Scopes.SINGLETON);


        Multibinder<Object> velocityListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("velocityListeners"));
        velocityListeners.addBinding().to(org.pytenix.chat.listener.PlayerDisconnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(ProxyPingListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerConnectionChangeListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerConnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerDisconnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerSettingsChangeListener.class).in(Scopes.SINGLETON);

        Multibinder<Object> omniListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("omniListeners"));
        omniListeners.addBinding().to(BackendCloseListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendConnectListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendMessageReceiveListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendConfigUpdateListener.class).in(Scopes.SINGLETON);

        bind(SystemChatPacketListener.class).in(Scopes.SINGLETON);

    }

    @Provides
    @Singleton
    public AbstractAnalyticsSecret provideAnalyticsSecret(Logger logger) {
        return new ProxyAnalyticsSecret(logger, dataDirectory);
    }

    @Provides
    @Singleton
    public TransportService<WebSocket> provideExternalTransportService(
            PacketRegistrar<WebSocket> packetRegistrar,
            WebSocketService webSocketService) {

        final TransportService<WebSocket> transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(500)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(50000)
                        .build())
                .encryptionEnabled(false)
                .networkSender(webSocketService::sendToWebSocket)
                .build();

        packetRegistrar.register(transportService);
        return transportService;
    }

    @Provides
    @Singleton
    public TransportService<RegisteredServer> provideInternalTransportService(
            @Named("forwardingSecret") String secret,
            @Named("channelIdentifier") ChannelIdentifier identifier,
            PacketRegistrar<RegisteredServer> packetRegistrar) {


        final TransportService<RegisteredServer> transportService = TransportService.<RegisteredServer>builder()
                .packetService(new DefaultPacketService<>())
                .secret(secret)
                .encryptionEnabled(true)
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(100)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(20000)
                        .build())
                .networkSender((PluginMessageSender<RegisteredServer>) (registeredServer, bytes) -> registeredServer.sendPluginMessage(identifier, bytes))
                .build();

        packetRegistrar.register(transportService);

        return transportService;
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
    public PlayerLocaleProcessor providePlayerLocaleProcessor(Provider<ROIService> roiServiceProvider) {
        return uuid ->
        {
            System.out.println("LOCALE: " + roiServiceProvider.get().getLanguageCache().get(uuid, uuid1 -> "en_en"));
            return roiServiceProvider.get().getLanguageCache().get(uuid, uuid1 -> "en_en");
        };
    }

    @Provides
    @Singleton
    public TranslationProcessor provideTranslationProcessor(Provider<TranslationSocketEndpoint> endpointProvider) {
        return (id, text, targetLang, module) -> endpointProvider.get().sendTranslationRequest(id, text, targetLang, module);
    }


    @Provides
    @Singleton
    public ProxyTransport provideProxyTransport(ProxyServer proxyServer, TransportService<RegisteredServer> transportService, ChannelIdentifier channelIdentifier) {
        return new ProxyTransport(plugin, transportService, channelIdentifier, proxyServer);
    }

    @Provides
    @Singleton
    public LimboService provideLimboService(
            ProxyServer proxyServer,
            ServerPreConnectListener preConnectListener,
            TranslateCommand translateCommand) {
        return new LimboService(plugin, proxyServer, 25588, forwardingSecret, preConnectListener, translateCommand);
    }
}