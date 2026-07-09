package org.omni.packets.data;

import java.util.UUID;

public record GeoRequestData(UUID requestId, String ipAddress) {
}
