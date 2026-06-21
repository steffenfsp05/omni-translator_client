package org.pytenix.packets.impl;

import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

public class ConfigRequestMapperAbstract extends AbstractPacketMapper<NetworkPackets.ConfigRequestPacket, ConfigRequestMapperAbstract.RequestData> {

    public ConfigRequestMapperAbstract() {
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
