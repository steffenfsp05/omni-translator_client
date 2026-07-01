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
                .setTranslationsActive(packet.translations_enabled())
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
                packet.getTranslationsActive(),
                packet.getLangDistributionMap());
    }

    public record HeartBeatData(String license, UUID requestId , Long timestamp, int total_online, int translations_enabled, Map<String,Integer> language_distribution) {
    }
}