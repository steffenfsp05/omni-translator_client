package org.pytenix.socket.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.backend.BackendConnectionConnectEvent;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class BackendConnectListener {

    final Provider<WebSocketService> webSocketService;
    final TransportService<WebSocket> transportService;


    @Inject
    public BackendConnectListener(Provider<WebSocketService> webSocketService, TransportService<WebSocket> transportService) {
        this.webSocketService = webSocketService;
        this.transportService = transportService;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendConnect(BackendConnectionConnectEvent event) {
        final WebSocket webSocket = event.webSocket();

        webSocketService.get().setWebSocket(webSocket);
        webSocketService.get().getConnectionStatus().set(true);
        transportService.connect(webSocket);
        transportService.ready(webSocket);
    }
}
