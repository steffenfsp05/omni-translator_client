package org.pytenix.event.register;

import org.pytenix.packets.impl.ConsentRefreshRequestMapper;

public record ConsentUpdateEvent(ConsentRefreshRequestMapper.Data profilePacket) {
}
