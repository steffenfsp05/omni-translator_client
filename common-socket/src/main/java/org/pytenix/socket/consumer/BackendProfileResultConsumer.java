package org.pytenix.socket.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.ProfileResultData;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendProfileResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.ProfilePacket, ProfileResultData> {

    private final ProfileEndpoint profileEndpoint;

    @Inject
    public BackendProfileResultConsumer(ProfileEndpoint profileEndpoint) {
        this.profileEndpoint = profileEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, ProfileResultData javaPacket) {
        profileEndpoint.handleIncoming(javaPacket);
    }
}