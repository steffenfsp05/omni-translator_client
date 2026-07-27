package org.omni.event.register.player;

import java.util.UUID;

public record PlayerConnectEvent(UUID playerId, String playerName, String locale) {
}
