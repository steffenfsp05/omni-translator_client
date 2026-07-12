package org.omni.packets.registry;

import org.transport.TransportService;

public interface PacketRegistrar<A> {
    void register(TransportService<A> transport);
}
