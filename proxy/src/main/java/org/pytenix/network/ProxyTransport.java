package org.pytenix.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import org.omni.packets.PacketRegistry;
import org.omni.packets.registry.PacketRegistrar;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;


@Singleton
public class ProxyTransport {

    private final ProxyServer proxyServer;
    @Getter
    private final TransportService<RegisteredServer> transportService;
    private final ChannelIdentifier identifier = MinecraftChannelIdentifier.from("translator:main");

    @Inject
    public ProxyTransport(TranslatorPlugin translatorPlugin, ProxyServer proxyServer, String secret, PacketRegistrar<RegisteredServer> packetRegistrar) {

        this.proxyServer = proxyServer;

        proxyServer.getChannelRegistrar().register(identifier);

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

        packetRegistrar.register(transportService);

        PluginMessageReceiver<RegisteredServer> receiver = PluginMessageReceiver.autoConnectBridge(transportService);

        proxyServer.getEventManager().register(translatorPlugin, new Object() {
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


    }


    public void broadcastConfigurationUpdate(Protobuf.ServerConfiguration packet) {
        System.out.println("SENDING ALL CFGS");
        for (RegisteredServer server : proxyServer.getAllServers()) {

            if (!server.getPlayersConnected().isEmpty()) {

                transportService.send(server, PacketRegistry.SERVER_CONFIG, packet);
            }
        }
    }

    public void shutdown() {
        transportService.close();
    }

}
