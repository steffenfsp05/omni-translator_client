package org.omni.event.register.player;

import java.util.UUID;

public record PlayerSettingsChangeEvent(UUID uuid, String newLocale) {
}
