package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ProfileInternRequestData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.transport.service.PacketContext;

@Singleton
public class InternProfileConsumer extends MappedPacketReceiveConsumer<RegisteredServer, Protobuf.ProfileInternRequest, ProfileInternRequestData> {

    private final ProfileService profileService;

    @Inject
    public InternProfileConsumer(ProfileService profileService) {
        this.profileService = profileService;
    }


    @Override
    public void handle(PacketContext<RegisteredServer> context, ProfileInternRequestData javaPacket) {


        profileService.retrieveProfile(javaPacket.playerId()).thenAccept(profileData ->
                reply(context, PacketRegistry.PROFILE, profileData.withRequestId(javaPacket.requestId())));

    }
}

