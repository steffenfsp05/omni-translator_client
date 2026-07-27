package org.omni.translation.locale;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerLocaleProcessor {

    CompletableFuture<String> retrieveLocale(UUID uuid);
}
