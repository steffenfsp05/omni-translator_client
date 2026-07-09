package org.pytenix.injection;

import com.google.inject.*;
import com.velocitypowered.api.proxy.ProxyServer;
import org.omni.cache.CacheProvider;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.config.ConfigService;
import org.omni.config.ConfigurationFile;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslationProcessor;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.GeoSocketEndpoint;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.backend.TranslationSocketEndpoint;
import org.pytenix.chat.MessageSequencer;
import org.pytenix.chat.SystemChatModule;
import org.pytenix.limbo.LimboService;
import org.pytenix.network.ProxyTransport;
import org.pytenix.tracking.ExternProfileService;
import org.pytenix.tracking.ProxyAnalyticsSecret;
import org.pytenix.tracking.ROIService;
import org.slf4j.Logger;

import java.nio.file.Path;

public class TranslatorProxyModule extends AbstractModule {

    private final TranslatorPlugin plugin;
    private final String forwardingSecret;
    private final Path dataDirectory;

    public TranslatorProxyModule(TranslatorPlugin plugin, String forwardingSecret, Path dataDirectory) {
        this.plugin = plugin;
        this.forwardingSecret = forwardingSecret;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {
        // Kern-Komponenten binden
        bind(TranslatorPlugin.class).toInstance(plugin);

        // 2. Velocity-spezifische Caches und Services
        bind(new TypeLiteral<CacheProvider<String, String>>(){}).to(new TypeLiteral<CaffeineCacheProvider<String, String>>(){}).in(Scopes.SINGLETON);

        bind(ProfileService.class).to(ExternProfileService.class).in(Scopes.SINGLETON);

        bind(MessageSequencer.class).in(Scopes.SINGLETON);
        bind(ROIService.class).in(Scopes.SINGLETON);
        bind(SystemChatModule.class).in(Scopes.SINGLETON);
        bind(GeoSocketEndpoint.class).in(Scopes.SINGLETON);

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
        return uuid -> roiServiceProvider.get().getLanguageCache().get(uuid, uuid1 -> "en_en");
    }

    @Provides
    @Singleton
    public TranslationProcessor provideTranslationProcessor(Provider<TranslationSocketEndpoint> endpointProvider) {
        return (id, text, targetLang, module) -> endpointProvider.get().sendTranslationRequest(id, text, targetLang, module);
    }

    @Provides
    @Singleton
    public ProxyTransport provideProxyTransport() {
        return new ProxyTransport(plugin, forwardingSecret);
    }

    @Provides
    @Singleton
    public LimboService provideLimboService(ProxyServer proxyServer) {
        return new LimboService(plugin, proxyServer, 25588, forwardingSecret);
    }

    @Provides
    @Singleton
    public OmniConnectionService provideOmniConnectionService(
            ProxyServer proxyServer,
            ConfigurationFile config,
            Provider<TranslationSocketEndpoint> translationProvider,
            Provider<GeoSocketEndpoint> geoProvider,
            Provider<ProfileService> profileProvider) {

        OmniConnectionService service = new OmniConnectionService(plugin, config.getLicenseKey(), proxyServer);
        service.setServices(translationProvider.get(), geoProvider.get(), profileProvider.get());
        return service;
    }
}