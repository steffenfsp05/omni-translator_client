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
                .setLicense(packet.license())
                .setConsentType(packet.consentType() == null ? Protobuf.ConsentType.UNKNOWN : packet.consentType())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .build();
    }

    @Override
    public ProfileResultData from(Protobuf.ProfilePacket packet) {
        return new ProfileResultData(
                packet.getLicense(),
                packet.getAnalyticsId().toByteArray(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getConsentType()
        );
    }


}
