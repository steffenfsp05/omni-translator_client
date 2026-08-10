package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.proto.generated.Protobuf;

@Singleton
public class ExternProfileUpdateMapper extends AbstractPacketMapper<Protobuf.ProfileUpdate, ProfileUpdateData> {


    final ProfileMapper profileMapper = new ProfileMapper();

    public ExternProfileUpdateMapper() {
        super(Protobuf.ProfileUpdate.class, ProfileUpdateData.class);
    }

    @Override
    public Protobuf.ProfileUpdate to(ProfileUpdateData packet) {

        final ProfileResultData profileData = packet.profileData();
        final Protobuf.ProfilePacket profilePacket = profileMapper.to(profileData);


        return Protobuf.ProfileUpdate.newBuilder()
                .setProfilePacket(profilePacket)
                .build();
    }

    @Override
    public ProfileUpdateData from(Protobuf.ProfileUpdate packet) {
        final Protobuf.ProfilePacket profileData = packet.getProfilePacket();

        return new ProfileUpdateData(
                profileMapper.from(profileData)
        );
    }


}
