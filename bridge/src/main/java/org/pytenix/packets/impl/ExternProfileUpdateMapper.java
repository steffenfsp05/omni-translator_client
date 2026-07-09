package org.pytenix.packets.impl;

import com.google.protobuf.ByteString;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class ExternProfileUpdateMapper extends AbstractPacketMapper<NetworkPackets.ProfileExternUpdate, ExternProfileUpdateMapper.ExternProfileUpdateData> {


    final ProfileMapper profileMapper = new ProfileMapper();

    public ExternProfileUpdateMapper() {
        super(NetworkPackets.ProfileExternUpdate.class, ExternProfileUpdateData.class);
    }

    @Override
    public NetworkPackets.ProfileExternUpdate to(ExternProfileUpdateData packet) {

        final ProfileMapper.ProfileData profileData = packet.profileData;
        final NetworkPackets.ProfilePacket profilePacket = profileMapper.to(profileData);


        return NetworkPackets.ProfileExternUpdate.newBuilder()
                .setProfilePacket(profilePacket)
                .build();
    }

    @Override
    public ExternProfileUpdateData from(NetworkPackets.ProfileExternUpdate packet) {
        final NetworkPackets.ProfilePacket profileData = packet.getProfilePacket();

        return new ExternProfileUpdateData(
                profileMapper.from(profileData)
        );
    }


    public record ExternProfileUpdateData(
            ProfileMapper.ProfileData profileData
    ) {

    }
}
