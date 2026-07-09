package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.GeoRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class GeoRequestMapper extends AbstractPacketMapper<Protobuf.GeoRequestPacket, GeoRequestData> {

    public GeoRequestMapper() {
        super(
                Protobuf.GeoRequestPacket.class,
                GeoRequestData.class
        );
    }

    @Override
    public Protobuf.GeoRequestPacket to(GeoRequestData data) {
        return Protobuf.GeoRequestPacket.newBuilder()
                .setRequestIdMostSig(data.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(data.requestId().getLeastSignificantBits())
                .setIpAddress(data.ipAddress())
                .build();
    }

    @Override
    public GeoRequestData from(Protobuf.GeoRequestPacket proto) {
        return new GeoRequestData(
                new UUID(proto.getRequestIdMostSig(), proto.getRequestIdLeastSig()),
                proto.getIpAddress()
        );
    }


}
