package org.omni.event.register.player;

import java.util.UUID;

public record PlayerDisconnectEvent(UUID playerId) {
}
