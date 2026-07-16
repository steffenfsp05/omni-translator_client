package org.omni.packets.impl;

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileExternRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class ExternProfileRequestMapper extends AbstractPacketMapper<Protobuf.ProfileExternRequest, ProfileExternRequestData> {


    public ExternProfileRequestMapper() {
        super(Protobuf.ProfileExternRequest.class, ProfileExternRequestData.class);
    }

    @Override
    public Protobuf.ProfileExternRequest to(ProfileExternRequestData packet) {
        return Protobuf.ProfileExternRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .build();
    }

    @Override
    public ProfileExternRequestData from(Protobuf.ProfileExternRequest packet) {
        return new ProfileExternRequestData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray()
        );
    }

}
