package org.pytenix.pluginmessage;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.pluginmessage.consumer.ConfigRequestConsumer;
import org.pytenix.pluginmessage.consumer.TranslationRequestConsumer;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyTransport {

    final TranslatorPlugin translatorPlugin;
    final TransportService<RegisteredServer> transportService;
    private final ChannelIdentifier identifier = MinecraftChannelIdentifier.from("translator:main");
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Translation-API-Worker");
        thread.setDaemon(true);
        return thread;
    });


    public ProxyTransport(TranslatorPlugin translatorPlugin, String secret) {

        this.translatorPlugin = translatorPlugin;

        translatorPlugin.getProxyServer().getChannelRegistrar().register(identifier);

        //TODO IMPLEMENT VELOCITY SECRET FROM CFG!
        this.transportService = TransportService.<RegisteredServer>builder()
                .packetService(new DefaultPacketService<>())
                .secret(secret)
                .encryptionEnabled(true)
                .options(
                        TransportOptions.builder()
                                .batchingEnabled(true)
                                .maxBatchSize(100)
                                .batchingIntervalMs(5)
                                .maxPayloadSize(20000)
                                .build()
                )
                .networkSender((PluginMessageSender<RegisteredServer>) (registeredServer, bytes) -> registeredServer.sendPluginMessage(identifier, bytes))
                .build();

        PluginMessageReceiver<RegisteredServer> receiver = PluginMessageReceiver.autoConnectBridge(transportService);

        translatorPlugin.getProxyServer().getEventManager().register(translatorPlugin, new Object() {
            @Subscribe
            public void onPluginMessage(PluginMessageEvent event) {

                if (event.getSource() instanceof ServerConnection serverConnection) {
                    if (event.getIdentifier().getId().equalsIgnoreCase(identifier.getId())) {
                        RegisteredServer server = serverConnection.getServer();
                        receiver.handle(server, event.getData());
                    }
                }
            }
        });

        this.transportService.registerPacket(PacketRegistry.TRANSLATION_RESULT, (stringPacketContext, translationResult) -> {
        });

        this.transportService.registerPacket(PacketRegistry.CONFIG_REQUEST, new ConfigRequestConsumer(translatorPlugin.getTranslatorService()));
        this.transportService.registerPacket(PacketRegistry.TRANSLATION_REQUEST, new TranslationRequestConsumer(translatorPlugin, apiExecutor));

    }

    public void broadcastConfigurationUpdate(NetworkPackets.ServerConfiguration packet) {
        for (RegisteredServer server : translatorPlugin.getProxyServer().getAllServers()) {

            if (!server.getPlayersConnected().isEmpty()) {

                transportService.send(server, PacketRegistry.SERVER_CONFIG, packet);
            }
        }
    }

    public void shutdown() {
        transportService.close();
        apiExecutor.shutdown();
    }

}
