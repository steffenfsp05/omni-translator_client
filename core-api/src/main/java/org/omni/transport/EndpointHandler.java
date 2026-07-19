package org.omni.transport;

import java.util.concurrent.CompletableFuture;

public interface EndpointHandler<I, O, R> {

    void handleIncoming(I inbound);

    CompletableFuture<R> sendRequest(O outbound);

    default void update(I inbound) {
    }
}
