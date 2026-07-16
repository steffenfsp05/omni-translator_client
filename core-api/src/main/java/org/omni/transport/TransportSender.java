package org.omni.transport;

import com.google.protobuf.MessageLite;
import org.transport.service.impl.PacketDefinition;

public interface TransportSender {

    <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, Record record);
}
