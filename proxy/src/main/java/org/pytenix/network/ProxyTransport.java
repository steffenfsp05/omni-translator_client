package org.pytenix.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import org.omni.packets.PacketRegistry;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;


@Singleton
public class ProxyTransport {

    private final ProxyServer proxyServer;
    @Getter
    private final TransportService<RegisteredServer> transportService;


    @Inject
    public ProxyTransport(
            TranslatorPlugin translatorPlugin,
            TransportService<RegisteredServer> transportService,
            ChannelIdentifier channelIdentifier,
            ProxyServer proxyServer
    ) {

        this.proxyServer = proxyServer;

        proxyServer.getChannelRegistrar().register(channelIdentifier);

        this.transportService = transportService;


        PluginMessageReceiver<RegisteredServer> receiver = PluginMessageReceiver.autoConnectBridge(transportService);

        proxyServer.getEventManager().register(translatorPlugin, new Object() {
            @Subscribe
            public void onPluginMessage(PluginMessageEvent event) {

                if (event.getSource() instanceof ServerConnection serverConnection) {
                    if (event.getIdentifier().getId().equalsIgnoreCase(channelIdentifier.getId())) {
                        RegisteredServer server = serverConnection.getServer();
                        receiver.handle(server, event.getData());
                    }
                }
            }
        });


    }


    public void broadcastConfigurationUpdate(Protobuf.ServerConfiguration packet) {
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
