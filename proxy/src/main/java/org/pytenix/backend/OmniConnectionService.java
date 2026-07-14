package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import org.omni.packets.PacketMapperRegistry;
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
    private final PacketMapperRegistry packetMapperRegistry;


    @Inject
    public OmniConnectionService(
            TransportService<WebSocket> transportService,
            WebSocketService webSocketService,
            PacketMapperRegistry packetMapperRegistry) {

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!!");


        this.packetMapperRegistry = packetMapperRegistry;
        this.webSocketService = webSocketService;
        this.transportService = transportService;


        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!! REGUSTERED PACKEST");
    }

    public void connect() {
        webSocketService.connect();
    }

    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, Object o) {

        if (webSocketService == null) return;
        if (!webSocketService.getConnectionStatus().get()) return;

        transportService.send(webSocketService.getWebSocket(), packetDefinition.id(), packetMapperRegistry.toProto(o));
    }


    public void shutdown() {
        if (transportService != null) transportService.close();
    }

}