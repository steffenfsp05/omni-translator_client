package org.omni.event.register.player;

import java.util.UUID;

public record OmniPlayerConnectEvent(UUID playerId, String playerName, String locale) {
}
