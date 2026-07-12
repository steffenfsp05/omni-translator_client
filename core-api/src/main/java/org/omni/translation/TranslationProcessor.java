package org.omni.translation;

import org.omni.entity.TranslationModule;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface TranslationProcessor {

    CompletableFuture<String> endpointTranslation(UUID id, String text, String targetLang, TranslationModule translationModule);

}
