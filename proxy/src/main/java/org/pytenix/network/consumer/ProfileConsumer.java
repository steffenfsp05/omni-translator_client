package org.pytenix.network.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.InternProfileRequestMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.profile.AnalyticsKey;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.service.PacketContext;

@RequiredArgsConstructor
public class ProfileConsumer implements MappedPacketReceiveConsumer<RegisteredServer, NetworkPackets.ProfileInternRequest, InternProfileRequestMapper.InternProfileData> {

    final TranslatorPlugin translatorPlugin;


    @Override
    public void handle(PacketContext<RegisteredServer> context, InternProfileRequestMapper.InternProfileData javaPacket) {

            System.out.println("INCOMING REQUEST FOR PROFILEDATE: " + javaPacket);

            translatorPlugin.getProfileService().retrieveProfile(javaPacket.playerId()).thenAccept(profileData ->
            {
                context.reply(PacketRegistry.PROFILE,
                        PacketMapperRegistry.toProto(
                                profileData
                                        .withRequestId(javaPacket.requestId())
                        )
                );
            });

    }
}

