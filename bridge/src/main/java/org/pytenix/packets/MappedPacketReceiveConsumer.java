package org.pytenix.packets;

import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

@FunctionalInterface
public interface MappedPacketReceiveConsumer<C, P, J> extends PacketReceiveConsumer<C, P> {

    // 1. Das ist jetzt die einzige abstrakte Methode (perfekt für Lambdas!)
    void handle(PacketContext<C> context, J javaPacket);

    // 2. Die Mapping-Logik verstecken wir in der default-Implementierung
    @Override
    default void accept(PacketContext<C> context, P protoPacket) {

        // Mappen (Protobuf -> Java)
        J javaPacket = PacketMapperRegistry.fromProto(protoPacket);

        // Lambda aufrufen
        handle(context, javaPacket);
    }
}