package org.pytenix.socket.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.EventService;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.proto.generated.Protobuf;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendCacheInvalidationConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.CacheInvalidationRequest, CacheInvalidationRequest> {

    final EventService eventService;


    @Inject
    public BackendCacheInvalidationConsumer(EventService eventService)
    {
        this.eventService = eventService;
    }


    @Override
    public void handle(PacketContext<WebSocket> context, CacheInvalidationRequest javaPacket) {
        this.eventService.callEvent(javaPacket);
    }
}
