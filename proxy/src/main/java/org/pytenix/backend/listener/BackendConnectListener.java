package org.pytenix.backend.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.BackendConnectionConnectEvent;
import org.pytenix.backend.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class BackendConnectListener {

    final WebSocketService webSocketService;
    final TransportService<WebSocket> transportService;


    @Inject
    public BackendConnectListener(WebSocketService webSocketService, TransportService<WebSocket> transportService) {
        this.webSocketService = webSocketService;
        this.transportService = transportService;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendConnect(BackendConnectionConnectEvent event) {
        final WebSocket webSocket = event.webSocket();

        webSocketService.setWebSocket(webSocket);
        webSocketService.getConnectionStatus().set(true);
        transportService.connect(webSocket);
        transportService.ready(webSocket);
    }
}
