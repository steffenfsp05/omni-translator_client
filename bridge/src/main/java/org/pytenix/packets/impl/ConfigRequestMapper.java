package org.pytenix.packets.impl;

import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

public class ConfigRequestMapper extends AbstractPacketMapper<NetworkPackets.ConfigRequestPacket, ConfigRequestMapper.RequestData> {

    public ConfigRequestMapper() {
        super(NetworkPackets.ConfigRequestPacket.class, RequestData.class);
    }

    @Override
    public NetworkPackets.ConfigRequestPacket to(RequestData packet) {
        return NetworkPackets.ConfigRequestPacket.newBuilder()
                .setTimestamp(packet.timestamp())
                .build();
    }

    @Override
    public RequestData from(NetworkPackets.ConfigRequestPacket packet) {
        return new RequestData(packet.getTimestamp());
    }

    public record RequestData(long timestamp){}
}
