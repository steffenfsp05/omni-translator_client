package org.pytenix.packets.impl;

import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
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
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .setPlaytimeSeconds(packet.playtimeSeconds())
                .setTimestamp(packet.timestamp())
                .build();
    }

    @Override
    public TrackData from(NetworkPackets.TrackPlayerPacket packet) {
        return new TrackData(
                packet.getLicenseKey(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                new UUID(packet.getUserIdMostSig() , packet.getUserIdLeastSig()),
                packet.getTimestamp(),
                packet.getPlaytimeSeconds()
        );
    }

    public record TrackData(String licenseKey, UUID requestId, UUID playerId, long timestamp, int playtimeSeconds)
    {

    }
}
