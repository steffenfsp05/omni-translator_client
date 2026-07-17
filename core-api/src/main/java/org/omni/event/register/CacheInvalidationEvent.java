package org.omni.event.register;

import org.omni.packets.data.CacheInvalidationRequest;

public record CacheInvalidationEvent(CacheInvalidationRequest cacheInvalidationRequest) {
}
