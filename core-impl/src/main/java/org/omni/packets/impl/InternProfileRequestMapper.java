package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileInternRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class InternProfileRequestMapper extends AbstractPacketMapper<Protobuf.ProfileInternRequest, ProfileInternRequestData> {


    public InternProfileRequestMapper() {
        super(Protobuf.ProfileInternRequest.class, ProfileInternRequestData.class);
    }

    @Override
    public Protobuf.ProfileInternRequest to(ProfileInternRequestData packet) {
        return Protobuf.ProfileInternRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .build();
    }

    @Override
    public ProfileInternRequestData from(Protobuf.ProfileInternRequest packet) {
        return new ProfileInternRequestData(
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig())
        );
    }


}
