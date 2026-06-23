package org.pytenix.event.register;

import org.pytenix.packets.impl.ConsentRefreshRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;

public record ConsentUpdateEvent( ConsentRefreshRequestMapper.Data profilePacket) {
}
