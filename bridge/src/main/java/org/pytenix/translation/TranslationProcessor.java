package org.pytenix.translation;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface TranslationProcessor {

    CompletableFuture<String> endpointTranslation(UUID id, String text, String targetLang, String module);

}
