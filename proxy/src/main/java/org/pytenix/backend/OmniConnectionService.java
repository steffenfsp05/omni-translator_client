package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import org.omni.packets.PacketMapperRegistry;
import org.pytenix.backend.socket.WebSocketService;
import org.transport.TransportService;
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



        this.packetMapperRegistry = packetMapperRegistry;
        this.webSocketService = webSocketService;
        this.transportService = transportService;


    }

    public void connect() {
        webSocketService.connect();
    }
    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, Record record) {
        if (record instanceof MessageLite)
            throw new IllegalArgumentException("Records dürfen keine MessageLite-Implementierungen sein!");


        if (webSocketService == null) return;
        if (!webSocketService.getConnectionStatus().get()) return;

        transportService.send(webSocketService.getWebSocket(), packetDefinition.id(), packetMapperRegistry.toProto(record));
    }


    public void shutdown() {
        if (transportService != null) transportService.close();
    }

}