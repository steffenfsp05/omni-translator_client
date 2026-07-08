package org.pytenix.network.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.service.PacketContext;

@RequiredArgsConstructor
public class ProfileConsumer implements MappedPacketReceiveConsumer<RegisteredServer, NetworkPackets.ProfilePacket, ProfileMapper.ProfileData> {

    final TranslatorPlugin translatorPlugin;


    @Override
    public void handle(PacketContext<RegisteredServer> context, ProfileMapper.ProfileData javaPacket) {

        switch (javaPacket.action())
        {
            case FETCH -> {
                System.out.println("INCOMING REQUEST FOR PROFILEDATE: " + javaPacket);
                translatorPlugin.getProfileService().retrieveProfile(javaPacket.playerId())
                        .thenAcceptAsync(profileData ->
                        {
                            System.out.println("RETURNING NOW: "+ profileData);
                            context.reply(PacketRegistry.PROFILE,
                                    PacketMapperRegistry.toProto(profileData.withRequestId(javaPacket.requestId()).withAction(NetworkPackets.ProfilePacket.Action.RESPONSE)));


                        }  );
            }
        }
    }
}

