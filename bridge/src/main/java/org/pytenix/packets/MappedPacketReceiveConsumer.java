package org.pytenix.packets;

import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

@FunctionalInterface
public interface MappedPacketReceiveConsumer<C, P, J> extends PacketReceiveConsumer<C, P> {

    void handle(PacketContext<C> context, J javaPacket);

    @Override
    default void accept(PacketContext<C> context, P protoPacket) {

        // Mappen (Protobuf -> Java)
        J javaPacket = PacketMapperRegistry.fromProto(protoPacket);

        handle(context, javaPacket);
    }
}