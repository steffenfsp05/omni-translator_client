package org.omni.event.register.player;

import org.omni.packets.data.ConsentRefreshRequestData;

public record ConsentUpdateEvent(ConsentRefreshRequestData refreshRequestData) {
}
