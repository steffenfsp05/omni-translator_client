package org.omni.event.register.player;

import java.util.UUID;

public record OmniPlayerSettingsChangeEvent(UUID uuid, String newLocale) {
}
