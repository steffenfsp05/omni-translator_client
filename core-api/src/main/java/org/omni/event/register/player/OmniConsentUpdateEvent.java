package org.omni.event.register.player;

import org.omni.packets.data.ConsentRefreshRequestData;

public record OmniConsentUpdateEvent(ConsentRefreshRequestData refreshRequestData) {
}
