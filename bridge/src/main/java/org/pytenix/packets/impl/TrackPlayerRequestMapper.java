package org.pytenix.packets.impl;

import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import com.google.protobuf.ByteString;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class TrackPlayerRequestMapper  extends AbstractPacketMapper<NetworkPackets.TrackPlayerPacket, TrackPlayerRequestMapper.TrackData> {


    public TrackPlayerRequestMapper() {
        super(NetworkPackets.TrackPlayerPacket.class, TrackPlayerRequestMapper.TrackData.class);
    }

    @Override
    public NetworkPackets.TrackPlayerPacket to(TrackData packet) {
        return NetworkPackets.TrackPlayerPacket.newBuilder()
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
    public TrackData from(NetworkPackets.TrackPlayerPacket packet) {
        return new TrackData(
                packet.getLicenseKey(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray(),
                packet.getTimestamp(),
                packet.getPlaytimeSeconds(),
                packet.getIsTranslated(),
                packet.getLanguage()
        );
    }

    public record TrackData(String licenseKey, UUID requestId, byte[] analyticId, long timestamp, int playtimeSeconds, boolean is_translated, String language)    {

    }
}
