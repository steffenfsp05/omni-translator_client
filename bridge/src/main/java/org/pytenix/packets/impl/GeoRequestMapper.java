package org.pytenix.packets.impl;

import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

// Beispiel-Entity für GeoRequest

public class GeoRequestMapper extends AbstractPacketMapper<NetworkPackets.GeoRequestPacket, GeoRequestMapper.RequestData> {

    public GeoRequestMapper() {
        super(
                NetworkPackets.GeoRequestPacket.class,
                RequestData.class
        );
    }

    @Override
    public NetworkPackets.GeoRequestPacket to(RequestData data) {
        return NetworkPackets.GeoRequestPacket.newBuilder()
                .setRequestIdMostSig(data.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(data.requestId().getLeastSignificantBits())
                .setIpAddress(data.ipAddress())
                .build();
    }

    @Override
    public RequestData from(NetworkPackets.GeoRequestPacket proto) {
        return new RequestData(
                new UUID(proto.getRequestIdMostSig(), proto.getRequestIdLeastSig()),
                proto.getIpAddress()
        );
    }

    public record RequestData(UUID requestId, String ipAddress) {}
}
