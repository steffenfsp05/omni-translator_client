package org.pytenix.socket.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.backend.BackendConnectionCloseEvent;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class BackendCloseListener {


    final Provider<WebSocketService> webSocketService;
    final TransportService<WebSocket> transportService;

    @Inject
    public BackendCloseListener(Provider<WebSocketService> webSocketService, TransportService<WebSocket> transportService) {
        this.webSocketService = webSocketService;
        this.transportService = transportService;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendClose(BackendConnectionCloseEvent event) {
        final WebSocket webSocket = event.webSocket();
        final int statusCode = event.statusCode();

        webSocketService.get().getConnectionStatus().set(false);
        transportService.disconnect(webSocket);

        if (statusCode == 1008) {
            System.err.println("[OmniTranslator] FATAL: Verbindung wegen Lizenzfehlern geschlossen. Kein Reconnect.");
            return;
        } else {
            System.err.println("[OmniTranslator] Verbindung zum Backend abgebrochen. Grund: " + event.reason() + " - " + event.statusCode());
        }
        webSocketService.get().scheduleReconnect();


    }

}
