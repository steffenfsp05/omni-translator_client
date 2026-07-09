package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.HeartBeatUpdateData;
import org.omni.proto.generated.Protobuf;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class HeartBeatRequestMapper extends AbstractPacketMapper<Protobuf.HeartbeatPacket, HeartBeatUpdateData> {


    private static final Map<Protobuf.Module, ServerConfiguration.Module> MODULE_MAP = new EnumMap<>(Protobuf.Module.class);

    public HeartBeatRequestMapper() {
        super(Protobuf.HeartbeatPacket.class, HeartBeatUpdateData.class);
    }

    @Override
    public Protobuf.HeartbeatPacket to(HeartBeatUpdateData packet) {

        return Protobuf.HeartbeatPacket.newBuilder()
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
    public HeartBeatUpdateData from(Protobuf.HeartbeatPacket packet) {

        return new HeartBeatUpdateData(
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

}