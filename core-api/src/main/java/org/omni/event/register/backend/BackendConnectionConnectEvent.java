package org.omni.event.register.backend;

import java.net.http.WebSocket;

public record BackendConnectionConnectEvent(WebSocket webSocket) {
}
