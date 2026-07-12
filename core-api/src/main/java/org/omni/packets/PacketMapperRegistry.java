package org.omni.packets;

public interface PacketMapperRegistry {
    <P, J> P toProto(J javaObject);

    <P, J> J fromProto(P protoObject);
}
