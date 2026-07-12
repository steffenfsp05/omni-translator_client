package org.pytenix.backend.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.ProfileResultData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendProfileResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.ProfilePacket, ProfileResultData> {

    private final ProfileService profileService;

    @Inject
    public BackendProfileResultConsumer(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, ProfileResultData javaPacket) {
        System.out.println("INCOMING: " + javaPacket);
        profileService.handleProfileResult(javaPacket);
    }
}