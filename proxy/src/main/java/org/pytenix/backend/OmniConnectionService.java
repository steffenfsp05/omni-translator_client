package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import org.omni.packets.registry.PacketRegistrar;
import org.pytenix.backend.socket.WebSocketService;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.service.impl.DefaultPacketService;
import org.transport.service.impl.PacketDefinition;

import java.net.http.WebSocket;

@Singleton
public class OmniConnectionService {

    private final TransportService<WebSocket> transportService;

    private final WebSocketService webSocketService;


    @Inject
    public OmniConnectionService(
            WebSocketService webSocketService,
            PacketRegistrar<WebSocket> packetRegistrar) {

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!!");


        this.webSocketService = webSocketService;


        this.transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(500)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(50000)
                        .build())
                .encryptionEnabled(false)
                .networkSender(webSocketService::sendToWebSocket)
                .build();

        packetRegistrar.register(transportService);

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!! REGUSTERED PACKEST");
    }

    public void connect() {
        webSocketService.connect();
    }

    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, MessageLite packet) {

        if (webSocketService == null) return;
        if (!webSocketService.getConnectionStatus().get()) return;

        transportService.send(webSocketService.getWebSocket(), packetDefinition.id(), packet);
    }


    public void shutdown() {
        if (transportService != null) transportService.close();
    }

}