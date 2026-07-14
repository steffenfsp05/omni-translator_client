package org.pytenix.backend.listener;

import com.google.inject.Inject;
import org.pytenix.backend.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;

public class BackendConnectListener {

    final WebSocketService webSocketService;
    final TransportService<WebSocket> transportService;


    @Inject
    public BackendConnectListener(WebSocketService webSocketService, TransportService<WebSocket> transportService) {
        this.webSocketService = webSocketService;
        this.transportService = transportService;
    }

    public void onBackendConnect(WebSocket webSocket) {
        webSocketService.setWebSocket(webSocket);
        webSocketService.getConnectionStatus().set(true);
        transportService.connect(webSocket);
        transportService.ready(webSocket);
    }
}
