package org.pytenix.pluginmessage;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.pytenix.VelocityTranslator;
import org.pytenix.packets.Packets;
import org.pytenix.pluginmessage.consumer.ConfigRequestConsumer;
import org.pytenix.pluginmessage.consumer.TranslationRequestConsumer;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyTransport {

    final VelocityTranslator velocityTranslator;
    final TransportService<RegisteredServer> transportService;
    private final ChannelIdentifier identifier = MinecraftChannelIdentifier.from("translator:main");
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Translation-API-Worker");
        thread.setDaemon(true);
        return thread;
    });


    public ProxyTransport(VelocityTranslator velocityTranslator, String secret) {

        this.velocityTranslator = velocityTranslator;

        velocityTranslator.getProxyServer().getChannelRegistrar().register(identifier);

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

        velocityTranslator.getProxyServer().getEventManager().register(velocityTranslator,new Object() {
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

        this.transportService.registerPacket(Packets.TRANSLATION_RESULT, (stringPacketContext, translationResult) -> {
        });

        this.transportService.registerPacket(Packets.CONFIG_REQUEST, new ConfigRequestConsumer(velocityTranslator.getTranslatorService()));
        this.transportService.registerPacket(Packets.TRANSLATION_REQUEST, new TranslationRequestConsumer(velocityTranslator, apiExecutor));

    }

    public void broadcastConfigurationUpdate(NetworkPackets.ServerConfiguration packet) {
        for (RegisteredServer server : velocityTranslator.getProxyServer().getAllServers()) {

            if (!server.getPlayersConnected().isEmpty()) {

                transportService.send(server, Packets.SERVER_CONFIG, packet);
            }
        }
    }

    public void shutdown() {
        transportService.close();
        apiExecutor.shutdown();
    }

}
