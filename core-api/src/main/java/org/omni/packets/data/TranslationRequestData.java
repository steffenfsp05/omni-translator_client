package org.omni.packets.data;

import org.omni.entity.ServerConfiguration;

import java.util.UUID;

public record TranslationRequestData(UUID requestId, String text, String targetLanguage,
                                     ServerConfiguration.Module module) {
}
