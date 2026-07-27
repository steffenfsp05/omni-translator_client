package org.pytenix.socket.endpoint;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDeduplicatingEndpoint<K, ReqID, V> {

    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlightFetches = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ReqID, CompletableFuture<V>> requestQueue = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ReqID, K> requestKeyMap = new ConcurrentHashMap<>();

    protected CompletableFuture<V> executeDeduplicated(
            K deduplicationKey,
            ReqID requestId,
            long timeoutSeconds,
            Runnable networkAction,
            V fallbackValue
    ) {
        V cachedResult = getFromCache(deduplicationKey);
        if (cachedResult != null) {
            return CompletableFuture.completedFuture(cachedResult);
        }

        return inFlightFetches.computeIfAbsent(deduplicationKey, key -> {
            CompletableFuture<V> future = new CompletableFuture<>();

            requestQueue.put(requestId, future);
            requestKeyMap.put(requestId, key);

            networkAction.run();

            return future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler/Timeout beim asynchronen Request: " + ex.getMessage());
                        inFlightFetches.remove(key);
                        requestQueue.remove(requestId);
                        requestKeyMap.remove(requestId);
                        return fallbackValue;
                    });
        });
    }


    protected void resolveIncomingByRequestId(ReqID requestId, V result) {
        CompletableFuture<V> future = requestQueue.remove(requestId);
        K deduplicationKey = requestKeyMap.remove(requestId);

        if (deduplicationKey != null) {
            inFlightFetches.remove(deduplicationKey);
            saveToCache(deduplicationKey, result);
        }

        if (future != null) {
            future.complete(result);
        }
    }

    protected abstract V getFromCache(K key);
    protected abstract void saveToCache(K key, V value);
}