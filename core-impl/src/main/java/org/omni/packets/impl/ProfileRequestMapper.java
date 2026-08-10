package org.omni.packets.impl;

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class ProfileRequestMapper extends AbstractPacketMapper<Protobuf.ProfileRequest, ProfileRequestData> {


    public ProfileRequestMapper() {
        super(Protobuf.ProfileRequest.class, ProfileRequestData.class);
    }

    @Override
    public Protobuf.ProfileRequest to(ProfileRequestData packet) {
        return Protobuf.ProfileRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .build();
    }

    @Override
    public ProfileRequestData from(Protobuf.ProfileRequest packet) {
        return new ProfileRequestData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray()
        );
    }

}
