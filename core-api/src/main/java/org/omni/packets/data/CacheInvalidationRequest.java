package org.omni.packets.data;

import org.omni.entity.TranslationModule;

import java.util.UUID;

public record CacheInvalidationRequest(
        UUID requestId,
        Payload payload
) {
    public sealed interface Payload permits Profile, Translation {
    }

    public record Profile(byte[] analyticId) implements Payload {
    }

    public record Translation(String text, String language, TranslationModule translationModule) implements Payload {
    }
}
