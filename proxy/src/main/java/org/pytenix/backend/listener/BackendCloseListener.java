package org.pytenix.backend.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.BackendConnectionCloseEvent;
import org.pytenix.backend.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class BackendCloseListener {


    final WebSocketService webSocketService;
    final TransportService<WebSocket> transportService;

    @Inject
    public BackendCloseListener(WebSocketService webSocketService, TransportService<WebSocket> transportService) {
        this.webSocketService = webSocketService;
        this.transportService = transportService;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendClose(BackendConnectionCloseEvent event) {
        final WebSocket webSocket = event.webSocket();
        final int statusCode = event.statusCode();

        webSocketService.getConnectionStatus().set(false);
        transportService.disconnect(webSocket);

        if (statusCode == 1008) {
            System.err.println("[OmniTranslator] FATAL: Verbindung wegen Lizenzfehlern geschlossen. Kein Reconnect.");
            return;
        }
        webSocketService.scheduleReconnect();


    }

}
