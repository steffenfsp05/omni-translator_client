package org.pytenix.packets.impl;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class ProfileMapper extends AbstractPacketMapper<NetworkPackets.ProfilePacket, ProfileMapper.ProfileData> {


    public ProfileMapper() {
        super(NetworkPackets.ProfilePacket.class, ProfileData.class);
    }

    @Override
    public NetworkPackets.ProfilePacket to(ProfileData packet) {
        return NetworkPackets.ProfilePacket.newBuilder()
                .setAction(packet.action())
                .setConsentType(packet.consentType())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .build();
    }

    @Override
    public ProfileData from(NetworkPackets.ProfilePacket packet) {
        return new ProfileData(
                packet.getAction(),
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getConsentType()
        );
    }

    public record ProfileData(
            NetworkPackets.ProfilePacket.Action action,
            UUID playerId,
            UUID requestId,
            NetworkPackets.ProfilePacket.ConsentType consentType
    ) {}
}
