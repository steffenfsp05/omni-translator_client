package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.entity.TranslationModule;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.HeartBeatUpdateData;
import org.omni.proto.generated.Protobuf;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class HeartBeatRequestMapper extends AbstractPacketMapper<Protobuf.HeartbeatPacket, HeartBeatUpdateData> {

    public HeartBeatRequestMapper() {
        super(Protobuf.HeartbeatPacket.class, HeartBeatUpdateData.class);
    }

    @Override
    public Protobuf.HeartbeatPacket to(HeartBeatUpdateData packet) {

        return Protobuf.HeartbeatPacket.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setTimestamp(packet.timestamp())
                .setTotalOnline(packet.totalOnline())

                .setTranslationUnknownCount(packet.translationUnknown())
                .setTranslationExplicitCount(packet.translationExplicit())
                .setTranslationAutoCount(packet.translationAuto())
                .setTranslationDeclinedCount(packet.translationDeclined())

                .setAnalyticsUnknownCount(packet.analyticsUnknown())
                .setAnalyticsExplicitCount(packet.analyticsExplicit())
                .setAnalyticsAutoCount(packet.analyticsAuto())
                .setAnalyticsDeclinedCount(packet.analyticsDeclined())

                .putAllLangDistribution(packet.languageDistribution())
                .build();
    }

    @Override
    public HeartBeatUpdateData from(Protobuf.HeartbeatPacket packet) {

        return new HeartBeatUpdateData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getTimestamp(),
                packet.getTotalOnline(),

                packet.getTranslationUnknownCount(),
                packet.getTranslationExplicitCount(),
                packet.getTranslationAutoCount(),
                packet.getTranslationDeclinedCount(),

                packet.getAnalyticsUnknownCount(),
                packet.getAnalyticsExplicitCount(),
                packet.getAnalyticsAutoCount(),
                packet.getAnalyticsDeclinedCount(),

                packet.getLangDistributionMap()
        );
    }
}