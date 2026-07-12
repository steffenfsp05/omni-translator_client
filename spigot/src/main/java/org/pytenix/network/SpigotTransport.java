package org.pytenix.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.packets.registry.PacketRegistrar;
import org.pytenix.TranslatorPlugin;
import org.pytenix.network.listener.ConfigUpdateListener;
import org.pytenix.network.listener.ConsentUpdateListener;
import org.pytenix.network.service.ChannelCarrierService;
import org.pytenix.network.service.TranslationRequestService;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SpigotTransport {

    public final String pluginMessagingChannel;
    @Getter
    private final TransportService<String> transportService;
    private final TranslatorPlugin plugin;
    @Setter
    public boolean hasConfiguration;

    private final TranslationRequestService translationRequestService;

    @Inject
    public SpigotTransport(
            TranslatorPlugin plugin,
            @Named("pluginMessagingChannel") String pluginMessagingChannel,
            TransportService<String> transportService,
            TranslationRequestService translationRequestService
    ) {
        this.plugin = plugin;
        this.pluginMessagingChannel = pluginMessagingChannel;
        this.transportService = transportService;
        this.translationRequestService = translationRequestService;
    }

    @Inject
    private void initRegistrations(
            ChannelCarrierService channelCarrierService,
            PacketRegistrar<String> packetRegistrar,
            EventService eventService,
            ConfigUpdateListener configUpdateListener,
            ConsentUpdateListener consentUpdateListener
    ) {

        eventService.register(configUpdateListener);
        eventService.register(consentUpdateListener);

        Bukkit.getPluginManager().registerEvents(channelCarrierService, plugin);

        packetRegistrar.register(transportService);

        registerChannels();
        transportService.connect(pluginMessagingChannel);
        System.out.println("ABABABBDBASHDABSDA SDHJASBDAIKSBJDHJAKBSDJHABSDA");
    }


    private void registerChannels() {
        PluginMessageReceiver<String> receiver = PluginMessageReceiver.zeroCopyBridge(transportService);

        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, pluginMessagingChannel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, pluginMessagingChannel,
                (ch, player, msg) -> {
                    if (ch.equalsIgnoreCase(pluginMessagingChannel)) {
                        transportService.ready(ch);
                        receiver.handle(ch, msg);
                    }
                });
    }

    public CompletableFuture<String> translate(UUID id, String text, String targetLang, TranslationModule translationModule) {
        return translationRequestService.translate(id, text, targetLang, translationModule);
    }
}