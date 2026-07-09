package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ProfileExternUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.proto.generated.Protobuf;

@Singleton
public class ExternProfileUpdateMapper extends AbstractPacketMapper<Protobuf.ProfileExternUpdate, ProfileExternUpdateData> {


    final ProfileMapper profileMapper = new ProfileMapper();

    public ExternProfileUpdateMapper() {
        super(Protobuf.ProfileExternUpdate.class, ProfileExternUpdateData.class);
    }

    @Override
    public Protobuf.ProfileExternUpdate to(ProfileExternUpdateData packet) {

        final ProfileResultData profileData = packet.profileData();
        final Protobuf.ProfilePacket profilePacket = profileMapper.to(profileData);


        return Protobuf.ProfileExternUpdate.newBuilder()
                .setProfilePacket(profilePacket)
                .build();
    }

    @Override
    public ProfileExternUpdateData from(Protobuf.ProfileExternUpdate packet) {
        final Protobuf.ProfilePacket profileData = packet.getProfilePacket();

        return new ProfileExternUpdateData(
                profileMapper.from(profileData)
        );
    }


}
