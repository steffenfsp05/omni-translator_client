package org.pytenix.packets.impl;

import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class InternProfileRequestMapper extends AbstractPacketMapper<NetworkPackets.ProfileInternRequest, InternProfileRequestMapper.InternProfileData> {


    public InternProfileRequestMapper() {
        super(NetworkPackets.ProfileInternRequest.class, InternProfileData.class);
    }

    @Override
    public NetworkPackets.ProfileInternRequest to(InternProfileData packet) {
        return NetworkPackets.ProfileInternRequest.newBuilder()
                .setLicense(packet.license())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .build();
    }

    @Override
    public InternProfileData from(NetworkPackets.ProfileInternRequest packet) {
        return new InternProfileData(
                packet.getLicense(),
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig())
        );
    }

    public record InternProfileData(
            String license,
            UUID playerId,
            UUID requestId
    ) {

    }
}
