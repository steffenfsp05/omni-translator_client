package org.pytenix.backend.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.omni.event.EventService;
import org.omni.event.register.BackendConnectionCloseEvent;
import org.omni.event.register.BackendConnectionConnectEvent;
import org.omni.event.register.BackendMessageReceiveEvent;
import org.pytenix.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

@Singleton
public class WebSocketListener implements WebSocket.Listener {


    private final FastByteArrayOutputStream buffer = new FastByteArrayOutputStream();

    private final EventService eventService;

    @Inject
    public WebSocketListener(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        eventService.callEvent(new BackendConnectionConnectEvent(webSocket));

        WebSocket.Listener.super.onOpen(webSocket);

    }

    @Override
    public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        byte[] chunk = new byte[data.remaining()];
        data.get(chunk);
        try {
            buffer.write(chunk);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        data.position(data.limit());

        if (last) {
            byte[] fullPayload = buffer.toByteArray();
            buffer.reset();
            ByteBuf nettyBuf = Unpooled.wrappedBuffer(fullPayload);

            eventService.callEvent(new BackendMessageReceiveEvent(webSocket, nettyBuf));
        }
        webSocket.request(1);

        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        eventService.callEvent(new BackendConnectionCloseEvent(webSocket, 0, error.getMessage()));
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        eventService.callEvent(new BackendConnectionCloseEvent(webSocket, statusCode, reason));
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}
