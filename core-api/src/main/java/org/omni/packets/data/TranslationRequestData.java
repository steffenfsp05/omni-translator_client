package org.omni.packets.data;

import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;

import java.util.UUID;

public record TranslationRequestData(UUID requestId, String text, String targetLanguage,
                                     TranslationModule module) {
}
