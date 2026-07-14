package org.omni.event.register;

import java.net.http.WebSocket;

public record BackendConnectionCloseEvent(WebSocket webSocket, int statusCode, String reason) {
}
