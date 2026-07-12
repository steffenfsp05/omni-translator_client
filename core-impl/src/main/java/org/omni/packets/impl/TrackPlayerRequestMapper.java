package org.omni.packets.impl;

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.TrackPlayerRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class TrackPlayerRequestMapper extends AbstractPacketMapper<Protobuf.TrackPlayerPacket, TrackPlayerRequestData> {


    public TrackPlayerRequestMapper() {
        super(Protobuf.TrackPlayerPacket.class, TrackPlayerRequestData.class);
    }

    @Override
    public Protobuf.TrackPlayerPacket to(TrackPlayerRequestData packet) {
        return Protobuf.TrackPlayerPacket.newBuilder()
                .setLicenseKey(packet.licenseKey())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .setPlaytimeSeconds(packet.playtimeSeconds())
                .setTimestamp(packet.timestamp())
                .setIsTranslated(packet.is_translated())
                .setLanguage(packet.language())
                .build();
    }

    @Override
    public TrackPlayerRequestData from(Protobuf.TrackPlayerPacket packet) {
        return new TrackPlayerRequestData(
                packet.getLicenseKey(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray(),
                packet.getTimestamp(),
                packet.getPlaytimeSeconds(),
                packet.getIsTranslated(),
                packet.getLanguage()
        );
    }


}
