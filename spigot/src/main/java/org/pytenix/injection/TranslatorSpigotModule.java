package org.pytenix.injection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.omni.cache.CacheProvider;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.event.EventService;
import org.omni.event.impl.DefaultEventService;
import org.omni.packets.registry.PacketRegistrar;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslationProcessor;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.listener.PlayerJoinQuitListener;
import org.pytenix.listener.PlayerLocaleChangeListener;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.gui.cache.ItemStackCache;
import org.pytenix.module.gui.listener.PacketListener;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.hologram.listener.EntityPacketListener;
import org.pytenix.module.player.LiveChatModule;
import org.pytenix.module.player.listener.AsyncPlayerChatListener;
import org.pytenix.network.DefaultPacketRegistrar;
import org.pytenix.network.SpigotTransport;
import org.pytenix.network.listener.ConfigUpdateListener;
import org.pytenix.network.listener.ConsentUpdateListener;
import org.pytenix.network.service.ChannelCarrierService;
import org.pytenix.network.service.TranslationRequestService;
import org.pytenix.service.InternProfileService;
import org.pytenix.service.ModuleService;
import org.pytenix.service.TaskScheduler;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

import java.io.File;
import java.nio.file.Path;

public class TranslatorSpigotModule extends AbstractModule {

    private final TranslatorPlugin plugin;
    private final String forwardingSecret;
    private final String channelName = "translator:main";

    public TranslatorSpigotModule(TranslatorPlugin plugin, String forwardingSecret, Path dataDirectory) {
        this.plugin = plugin;
        this.forwardingSecret = forwardingSecret;
    }

    @Override
    protected void configure() {
        bind(TranslatorPlugin.class).toInstance(plugin);
        bind(Plugin.class).toInstance(plugin);

        bind(String.class).annotatedWith(Names.named("pluginMessagingChannel")).toInstance(channelName);
        bind(ObjectMapper.class).in(Scopes.SINGLETON);


        bind(new TypeLiteral<CacheProvider<String, String>>() {
        }).to(new TypeLiteral<CaffeineCacheProvider<String, String>>() {
        }).in(Scopes.SINGLETON);


        bind(ProfileService.class).to(InternProfileService.class).in(Scopes.SINGLETON);

        bind(new TypeLiteral<PacketRegistrar<String>>() {
        }).to(DefaultPacketRegistrar.class).in(Scopes.SINGLETON);

        Multibinder<AbstractTranslatorModule> moduleBinder = Multibinder.newSetBinder(binder(), AbstractTranslatorModule.class);
        moduleBinder.addBinding().to(InventoryModule.class);
        moduleBinder.addBinding().to(LiveChatModule.class);
        moduleBinder.addBinding().to(HologramModule.class);



    }

    @Provides
    @Singleton
    @Named("configFile")
    public File provideProxySyncConfigFile() {
        return new File(plugin.getDataFolder(), "proxy_sync_config.json");
    }



    @Provides
    @Singleton
    public PlayerLocaleProcessor providePlayerLocaleProcessor() {
        return uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            return (player == null) ? "en-en" : player.getLocale();
        };
    }

    @Provides
    @Singleton
    public TranslationProcessor provideTranslationProcessor(SpigotTransport transport) {
        return transport::translate;
    }

    @Provides
    @Singleton
    public TransportService<String> provideTransportService(
            TranslatorPlugin plugin,
            @Named("pluginMessagingChannel") String channel,
            ChannelCarrierService carrierManager,
            TaskScheduler taskScheduler
    ) {
        return TransportService.<String>builder()
                .packetService(new DefaultPacketService<>())
                .secret(forwardingSecret)
                .encryptionEnabled(true)
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(100)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(20000)
                        .build())
                .networkSender((PluginMessageSender<String>) (s, bytes) -> {
                    carrierManager.getRandomCarrier().ifPresent(carrier -> {
                        taskScheduler.runForEntity(carrier, () ->
                                carrier.sendPluginMessage(plugin, channel, bytes));
                    });
                })
                .build();
    }
}