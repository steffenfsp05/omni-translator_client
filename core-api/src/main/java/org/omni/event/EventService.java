package org.omni.event;

import java.util.concurrent.CompletableFuture;

public interface EventService {


    void register(Object listener);

    void unregister(Object listener);

    <T> T callEvent(T event);

    <T> CompletableFuture<T> callEventAsync(T event);
}
