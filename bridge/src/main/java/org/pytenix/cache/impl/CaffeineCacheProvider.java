package org.pytenix.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.cache.CacheProvider;

import java.time.Duration;

public class CaffeineCacheProvider<A,B> implements CacheProvider<A, B> {

    private final Cache<A, B> translationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();


    @Override
    public void set(A key, B value) {
        translationCache.put(key, value);
    }

    @Override
    public B get(A key) {
        return translationCache.getIfPresent(key);
    }

    @Override
    public void invalidate(A key) {
        translationCache.invalidate(key);
    }

    @Override
    public boolean exists(A key) {
        return translationCache.getIfPresent(key) != null;
    }
}
