package org.omni.packets;


import com.google.inject.Inject;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

public abstract class MappedPacketReceiveConsumer<C, P, J> implements PacketReceiveConsumer<C, P> {

    @Inject
    protected PacketMapperRegistry packetMapperRegistry;

    @Override
    public void accept(PacketContext<C> context, P protoPacket) {

        J javaPacket = packetMapperRegistry.fromProto(protoPacket);
        handle(context, javaPacket);
    }

    public abstract void handle(PacketContext<C> context, J javaPacket);

}