package org.pytenix.injection;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.omni.config.ConfigService;
import org.omni.config.ConfigurationFile;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.listener.*;
import org.pytenix.module.chat.listener.SystemChatPacketListener;
import org.pytenix.limbo.LimboService;
import org.pytenix.command.TranslateCommand;
import org.pytenix.limbo.listener.ServerPreConnectListener;

import java.util.Optional;

public class TranslatorProxyModule extends AbstractModule {

    private final TranslatorPlugin plugin;

    private final String forwardingSecret;


    public TranslatorProxyModule(TranslatorPlugin plugin, String forwardingSecret) {
        this.plugin = plugin;
        this.forwardingSecret = forwardingSecret;
    }

    @Override
    protected void configure() {


        bind(String.class).annotatedWith(Names.named("forwardingSecret")).toInstance(forwardingSecret);





        Multibinder<Object> velocityListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("velocityListeners"));
        velocityListeners.addBinding().to(org.pytenix.module.chat.listener.PlayerDisconnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(ProxyPingListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerConnectionChangeListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerConnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerDisconnectListener.class).in(Scopes.SINGLETON);
        velocityListeners.addBinding().to(PlayerSettingsChangeListener.class).in(Scopes.SINGLETON);


        bind(SystemChatPacketListener.class).in(Scopes.SINGLETON);

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
    public PlayerLocaleProcessor providePlayerLocaleProcessor(ProxyServer proxyServer) {
        return uuid ->
        {
            final Optional<Player> player = proxyServer.getPlayer(uuid);
            return player.map(value -> value.getPlayerSettings().getLocale().toString()).orElse("en_en");
        };
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