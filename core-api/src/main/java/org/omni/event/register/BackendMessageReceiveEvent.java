package org.omni.event.register;

import io.netty.buffer.ByteBuf;

import java.net.http.WebSocket;

public record BackendMessageReceiveEvent(WebSocket webSocket, ByteBuf data) {
}
