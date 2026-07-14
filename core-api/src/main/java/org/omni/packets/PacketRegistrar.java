package org.omni.packets;

import org.transport.TransportService;

public interface PacketRegistrar<A> {
    void register(TransportService<A> transport);
}
