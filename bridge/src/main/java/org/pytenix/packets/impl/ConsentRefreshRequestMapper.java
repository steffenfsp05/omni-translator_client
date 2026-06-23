package org.pytenix.packets.impl;


import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class ConsentRefreshRequestMapper extends AbstractPacketMapper<NetworkPackets.ConsentRefreshRequest, ConsentRefreshRequestMapper.Data> {


    public ConsentRefreshRequestMapper() {
        super(NetworkPackets.ConsentRefreshRequest.class, ConsentRefreshRequestMapper.Data.class);
    }


    @Override
    public NetworkPackets.ConsentRefreshRequest to(Data packet) {
        return NetworkPackets.ConsentRefreshRequest.newBuilder()
                .setConsentType(packet.consentType)
                .setUserIdMostSig(packet.playerId.getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId.getLeastSignificantBits())
                .setRequestIdMostSig(packet.requestId.getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId.getLeastSignificantBits())
                .build();
    }

    @Override
    public Data from(NetworkPackets.ConsentRefreshRequest packet) {
        return new ConsentRefreshRequestMapper.Data(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                packet.getConsentType()
        );
    }


    public record Data(
            UUID requestId,
            UUID playerId,
            NetworkPackets.ProfilePacket.ConsentType consentType
    )

    {

    }
}