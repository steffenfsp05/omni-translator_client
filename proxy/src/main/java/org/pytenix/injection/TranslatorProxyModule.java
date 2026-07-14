package org.pytenix.injection;

import com.google.inject.*;
import com.google.inject.name.Names;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
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
import org.pytenix.limbo.LimboService;
import org.pytenix.limbo.command.TranslateCommand;
import org.pytenix.limbo.listener.ServerPreConnectListener;
import org.pytenix.network.InternPacketRegistrar;
import org.pytenix.network.ProxyTransport;
import org.pytenix.tracking.ExternProfileService;
import org.pytenix.tracking.ProxyAnalyticsSecret;
import org.pytenix.tracking.ROIService;
import org.slf4j.Logger;

import java.net.http.WebSocket;
import java.nio.file.Path;

public class TranslatorProxyModule extends AbstractModule {

    private final TranslatorPlugin plugin;
    private final String forwardingSecret;
    private final Path dataDirectory;

    private final String backendRemoteAddress = "ws://192.168.178.121:8083/ws/omni";

    public TranslatorProxyModule(TranslatorPlugin plugin, String forwardingSecret, Path dataDirectory) {
        this.plugin = plugin;
        this.forwardingSecret = forwardingSecret;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {

        bind(String.class).annotatedWith(Names.named("backendRemoteAddress")).toInstance(backendRemoteAddress);

        bind(new TypeLiteral<CacheProvider<String, String>>() {}).to(new TypeLiteral<CaffeineCacheProvider<String, String>>() {}).in(Scopes.SINGLETON);

        bind(ProfileService.class).to(ExternProfileService.class).in(Scopes.SINGLETON);

        bind(new TypeLiteral<PacketRegistrar<RegisteredServer>>() {}).to(InternPacketRegistrar.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<PacketRegistrar<WebSocket>>() {}).to(ExternPacketRegistrar.class).in(Scopes.SINGLETON);

    }

    @Provides
    @Singleton
    public AbstractAnalyticsSecret provideAnalyticsSecret(Logger logger) {
        return new ProxyAnalyticsSecret(logger, dataDirectory);
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
    public ProxyTransport provideProxyTransport(  ProxyServer proxyServer, PacketRegistrar<RegisteredServer> registrar) {
        return new ProxyTransport(plugin, proxyServer, forwardingSecret, registrar);
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