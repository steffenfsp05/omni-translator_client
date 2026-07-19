package org.pytenix.socket.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.omni.event.EventService;
import org.omni.event.register.backend.BackendConnectionCloseEvent;
import org.omni.event.register.backend.BackendConnectionConnectEvent;
import org.omni.event.register.backend.BackendMessageReceiveEvent;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class WebSocketListener implements WebSocket.Listener {

    private final EventService eventService;

    private final Map<WebSocket, ByteBuf> bufferMap = new ConcurrentHashMap<>();

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
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        ByteBuf buffer = bufferMap.computeIfAbsent(webSocket, k -> Unpooled.buffer());

        buffer.writeBytes(data);

        if (last) {
            bufferMap.remove(webSocket);

            eventService.callEvent(new BackendMessageReceiveEvent(webSocket, buffer));
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        ByteBuf buf = bufferMap.remove(webSocket);
        if (buf != null) buf.release();

        eventService.callEvent(new BackendConnectionCloseEvent(webSocket, 0, error.getMessage()));
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        ByteBuf buf = bufferMap.remove(webSocket);
        if (buf != null) buf.release();

        eventService.callEvent(new BackendConnectionCloseEvent(webSocket, statusCode, reason));
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}