package org.omni.transport;

import com.google.protobuf.MessageLite;
import org.omni.entity.TranslationModule;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EndpointHandler<I, O, R> {

    void handleIncoming(I inbound) ;
    CompletableFuture<R> sendRequest(O outbound);

    default void update(I inbound)
    {
        return;
    }
}
