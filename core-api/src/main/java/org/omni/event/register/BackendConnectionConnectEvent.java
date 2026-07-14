package org.omni.event.register;

import java.net.http.WebSocket;

public record BackendConnectionConnectEvent(WebSocket webSocket) {
}
