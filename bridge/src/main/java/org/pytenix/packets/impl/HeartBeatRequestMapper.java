package org.pytenix.packets.impl;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartBeatRequestMapper extends AbstractPacketMapper<NetworkPackets.HeartbeatPacket, HeartBeatRequestMapper.HeartBeatData> {

    private static final Map<NetworkPackets.Module, ServerConfiguration.Module> MODULE_MAP = new EnumMap<>(NetworkPackets.Module.class);

    public HeartBeatRequestMapper() {
        super(NetworkPackets.HeartbeatPacket.class, HeartBeatRequestMapper.HeartBeatData.class);
    }

    @Override
    public NetworkPackets.HeartbeatPacket to(HeartBeatData packet) {

        return NetworkPackets.HeartbeatPacket.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setLicenseKey(packet.license())
                .setTimestamp(packet.timestamp())
                .setTotalOnline(packet.total_online())
                .setConsentUnknownCount(packet.consent_unknown())
                .setConsentExplicitCount(packet.consent_explicit())
                .setConsentAutoCount(packet.consent_auto())
                .setConsentDeclinedCount(packet.consent_declined())
                .putAllLangDistribution(packet.language_distribution())
                .build();
    }

    @Override
    public HeartBeatData from(NetworkPackets.HeartbeatPacket packet) {

        return new HeartBeatData(
                packet.getLicenseKey(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getTimestamp(),
                packet.getTotalOnline(),
                packet.getConsentUnknownCount(),
                packet.getConsentExplicitCount(),
                packet.getConsentAutoCount(),
                packet.getConsentDeclinedCount(),
                packet.getLangDistributionMap());
    }

    public record HeartBeatData(
            String license,
            UUID requestId,
            Long timestamp,
            int total_online,
            int consent_unknown,
            int consent_explicit,
            int consent_auto,
            int consent_declined,
            Map<String,Integer> language_distribution
    ) {
    }
}