package org.pytenix.packets.impl;

import org.pytenix.packets.PacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class GeoResultMapper extends PacketMapper<NetworkPackets.GeoResultPacket, GeoResultMapper.ResultData> {

    public GeoResultMapper() {
        super(NetworkPackets.GeoResultPacket.class, ResultData.class);
    }

    @Override
    public NetworkPackets.GeoResultPacket to(ResultData data) {
        return NetworkPackets.GeoResultPacket.newBuilder()
                .setRequestIdMostSig(data.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(data.requestId().getLeastSignificantBits())
                .setLanguage(data.language)
                .build();
    }

    @Override
    public ResultData from(NetworkPackets.GeoResultPacket packet) {
        return new ResultData(new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()), packet.getLanguage());
    }

    public record ResultData(UUID requestId, String language){}
}
