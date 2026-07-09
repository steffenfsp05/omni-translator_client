package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ProfileInternRequestData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;
import org.transport.service.PacketContext;

@Singleton
public class ProfileConsumer extends MappedPacketReceiveConsumer<RegisteredServer, Protobuf.ProfileInternRequest, ProfileInternRequestData> {

    private final TranslatorPlugin translatorPlugin;
    private final ProfileService profileService;
    private final PacketMapperRegistry packetMapperRegistry;

    @Inject
    public ProfileConsumer(TranslatorPlugin translatorPlugin, ProfileService profileService, PacketMapperRegistry packetMapperRegistry) {
        this.translatorPlugin = translatorPlugin;
        this.profileService = profileService;
        this.packetMapperRegistry = packetMapperRegistry;
    }


    @Override
    public void handle(PacketContext<RegisteredServer> context, ProfileInternRequestData javaPacket) {

            System.out.println("INCOMING REQUEST FOR PROFILEDATE: " + javaPacket);

        profileService.retrieveProfile(javaPacket.playerId()).thenAccept(profileData ->
            {
                context.reply(PacketRegistry.PROFILE,
                        packetMapperRegistry.toProto(
                                profileData
                                        .withRequestId(javaPacket.requestId())
                        )
                );
            });

    }
}

