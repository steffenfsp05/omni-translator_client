package org.omni.packets.impl;

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileResultData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class ProfileMapper extends AbstractPacketMapper<Protobuf.ProfilePacket, ProfileResultData> {


    public ProfileMapper() {
        super(Protobuf.ProfilePacket.class, ProfileResultData.class);
    }

    @Override
    public Protobuf.ProfilePacket to(ProfileResultData packet) {
        return Protobuf.ProfilePacket.newBuilder()
                .setTranslationConsentType(packet.translationConsent() == null ? Protobuf.ConsentType.UNKNOWN : packet.translationConsent())
                .setAnalyticsConsentType(packet.analyticConsent() == null ? Protobuf.ConsentType.UNKNOWN : packet.analyticConsent())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .setTranslationsAcceptedTimestamp(packet.translationsAcceptedTimestamp())
                .setAnalyticsAcceptedTimestamp(packet.analyticsAcceptedTimestamp())
                .build();
    }

    @Override
    public ProfileResultData from(Protobuf.ProfilePacket packet) {
        return new ProfileResultData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray(),
                packet.getTranslationConsentType(),
                packet.getAnalyticsConsentType(),
                packet.getAnalyticsAcceptedTimestamp(),
                packet.getTranslationsAcceptedTimestamp()
        );
    }


}
