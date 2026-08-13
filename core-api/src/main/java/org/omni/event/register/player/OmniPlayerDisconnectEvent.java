package org.omni.event.register.player;

import java.util.UUID;

public record OmniPlayerDisconnectEvent(UUID playerId, String playerName) {
}