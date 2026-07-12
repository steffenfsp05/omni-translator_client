package org.pytenix.backend.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.GeoResultData;
import org.omni.proto.generated.Protobuf;
import org.pytenix.backend.GeoSocketEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendGeoResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.GeoResultPacket, GeoResultData> {

    private final GeoSocketEndpoint geoSocketEndpoint;

    @Inject
    public BackendGeoResultConsumer(GeoSocketEndpoint geoSocketEndpoint) {
        this.geoSocketEndpoint = geoSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, GeoResultData javaPacket) {
        geoSocketEndpoint.handleGeoResult(javaPacket);
    }
}