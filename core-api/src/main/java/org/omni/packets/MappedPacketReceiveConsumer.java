package org.omni.packets;


import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;
import org.transport.service.impl.PacketDefinition;

public abstract class MappedPacketReceiveConsumer<C, P, J> implements PacketReceiveConsumer<C, P> {

    @Inject
    protected PacketMapperRegistry packetMapperRegistry;

    @Override
    public void accept(PacketContext<C> context, P protoPacket) {

        J javaPacket = packetMapperRegistry.fromProto(protoPacket);
        handle(context, javaPacket);
    }

    public void reply(PacketContext<C> context, PacketDefinition<? extends MessageLite> packetDefinition, Object javaPacket) {
        context.reply(packetDefinition, packetMapperRegistry.toProto(javaPacket));
    }

    public abstract void handle(PacketContext<C> context, J javaPacket);

}