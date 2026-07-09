package org.omni.event.register;

import org.omni.entity.ServerConfiguration;

public record ConfigUpdateEvent(ServerConfiguration translationConfiguration) {
}
