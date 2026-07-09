package org.omni.event.register;

import org.omni.packets.data.ConsentRefreshRequestData;

public record ConsentUpdateEvent(ConsentRefreshRequestData refreshRequestData) {
}
