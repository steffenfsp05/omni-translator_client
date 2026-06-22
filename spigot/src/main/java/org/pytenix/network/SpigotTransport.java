package org.pytenix.network;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.network.consumer.ConfigUpdateConsumer;
import org.pytenix.network.listener.ConfigUpdateListener;
import org.pytenix.packets.impl.TranslationResultMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpigotTransport {


    @Getter
    final TransportService<String> transportService;
    private final TranslatorPlugin plugin;
    private final ChannelCarrierService channelCarrierService;
    private final TranslationRequestService translationRequestService;


    @Setter
    public boolean hasConfiguration;

    public String pluginMessagingChannel;

    public Set<UUID> availableCarriers;

    public SpigotTransport(TranslatorPlugin plugin, String secret, String pluginMessagingChannel) {

        this.pluginMessagingChannel = pluginMessagingChannel;
        this.plugin = plugin;
        this.hasConfiguration = false;
        this.availableCarriers = new HashSet<>();



        this.channelCarrierService = new ChannelCarrierService(pluginMessagingChannel);
        this.translationRequestService = new TranslationRequestService(this, pluginMessagingChannel);
        this.transportService = TransportFactory.createSpigotTransport(secret, pluginMessagingChannel, plugin, channelCarrierService);

        registerEvents();
        registerChannels();

        transportService.connect(pluginMessagingChannel);


        registerPacketHandlers();


    }

    private void registerPacketHandlers()
    {
        this.transportService.registerPacket(PacketRegistry.TRANSLATION_RESULT,
                (MappedPacketReceiveConsumer<String, NetworkPackets.TranslationResult, TranslationResultMapper.ResultData>)
                        (context, resultData) ->
                                translationRequestService.completeRequest(
                                  resultData.requestId(),
                                  resultData.result()
                               ));

        this.transportService.registerPacket(PacketRegistry.SERVER_CONFIG,
                new ConfigUpdateConsumer(plugin, plugin.getTranslatorService()
                )
        );


        this.transportService.registerPacket(PacketRegistry.TRANSLATION_REQUEST,(stringPacketContext, translationRequest) -> {});
        this.transportService.registerPacket(PacketRegistry.CONFIG_REQUEST,(stringPacketContext, translationRequest) -> {});
    }

    private void registerEvents()
    {
        plugin.getTranslatorService().getEventService().register(new ConfigUpdateListener(plugin));

        Bukkit.getPluginManager().registerEvents(channelCarrierService, plugin);
    }

    private void registerChannels()
    {
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

    public CompletableFuture<String> translate(UUID id, String text, String targetLang, String module) {
       return translationRequestService.translate(id, text, targetLang, module);
    }



}
