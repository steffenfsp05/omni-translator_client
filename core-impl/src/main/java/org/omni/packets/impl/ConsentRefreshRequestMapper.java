package org.omni.packets.impl;


import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class ConsentRefreshRequestMapper extends AbstractPacketMapper<Protobuf.ConsentRefreshRequest, ConsentRefreshRequestData> {


    public ConsentRefreshRequestMapper() {
        super(Protobuf.ConsentRefreshRequest.class, ConsentRefreshRequestData.class);
    }


    @Override
    public Protobuf.ConsentRefreshRequest to(ConsentRefreshRequestData packet) {
        return Protobuf.ConsentRefreshRequest.newBuilder()
                .setAnalyticsConsentType(packet.analyticConsentType())
                .setTranslationConsentType(packet.translationConsentType())
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .build();
    }

    @Override
    public ConsentRefreshRequestData from(Protobuf.ConsentRefreshRequest packet) {
        return new ConsentRefreshRequestData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                packet.getTranslationConsentType(),
                packet.getAnalyticsConsentType()
        );
    }


}