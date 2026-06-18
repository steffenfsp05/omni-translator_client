package org.pytenix.event.register;

import org.pytenix.entity.ServerConfiguration;

public record ConfigUpdateEvent(ServerConfiguration translationConfiguration) {
}
