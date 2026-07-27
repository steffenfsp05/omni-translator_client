package org.omni.locale;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Singleton;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class LocaleManager {

    private static final String DEFAULT_FALLBACK_LOCALE = "en_us";


    private final Cache<UUID, CompletableFuture<String>> localeCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .build();


    public CompletableFuture<String> getLocale(UUID uuid) {
        return localeCache.get(uuid, k -> {
            CompletableFuture<String> pendingFuture = new CompletableFuture<>();

            pendingFuture.completeOnTimeout(DEFAULT_FALLBACK_LOCALE, 2, TimeUnit.SECONDS);

            return pendingFuture;
        });
    }


    public void updateLocale(UUID uuid, String newLocale) {
        CompletableFuture<String> future = localeCache.getIfPresent(uuid);

        if (future != null && !future.isDone()) {
            future.complete(newLocale);
        } else {
            localeCache.put(uuid, CompletableFuture.completedFuture(newLocale));
        }
    }


    public void cleanupPlayer(UUID uuid) {
        CompletableFuture<String> future = localeCache.getIfPresent(uuid);

        if (future != null && !future.isDone()) {
            future.complete(DEFAULT_FALLBACK_LOCALE);
        }

        localeCache.invalidate(uuid);
    }

}
