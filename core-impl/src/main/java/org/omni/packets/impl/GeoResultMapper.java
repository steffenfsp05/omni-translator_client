package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.GeoResultData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class GeoResultMapper extends AbstractPacketMapper<Protobuf.GeoResultPacket, GeoResultData> {

    public GeoResultMapper() {
        super(Protobuf.GeoResultPacket.class, GeoResultData.class);
    }

    @Override
    public Protobuf.GeoResultPacket to(GeoResultData data) {
        return Protobuf.GeoResultPacket.newBuilder()
                .setRequestIdMostSig(data.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(data.requestId().getLeastSignificantBits())
                .setLanguage(data.language())
                .build();
    }

    @Override
    public GeoResultData from(Protobuf.GeoResultPacket packet) {
        return new GeoResultData(new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()), packet.getLanguage());
    }

}
