package org.pytenix.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.cache.CacheProvider;

import java.time.Duration;

public class CaffeineCacheProvider implements CacheProvider<String, String> {

    private final Cache<String, String> translationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();


    @Override
    public void set(String key, String value) {
        translationCache.put(key, value);
    }

    @Override
    public String get(String key) {
        return translationCache.getIfPresent(key);
    }

    @Override
    public void invalidate(String key) {
        translationCache.invalidate(key);
    }

    @Override
    public boolean exists(String key) {
        return translationCache.getIfPresent(key) != null;
    }
}
