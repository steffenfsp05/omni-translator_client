package org.omni.packets.impl;


import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.proto.generated.Protobuf;


@Singleton
public class ConfigRequestMapper extends AbstractPacketMapper<Protobuf.ConfigRequestPacket, ConfigurationRequestData> {

    public ConfigRequestMapper() {
        super(Protobuf.ConfigRequestPacket.class, ConfigurationRequestData.class);
    }

    @Override
    public Protobuf.ConfigRequestPacket to(ConfigurationRequestData packet) {
        return Protobuf.ConfigRequestPacket.newBuilder()
                .setTimestamp(packet.timestamp())
                .build();
    }

    @Override
    public ConfigurationRequestData from(Protobuf.ConfigRequestPacket packet) {
        return new ConfigurationRequestData(packet.getTimestamp());
    }

}
