package org.pytenix.backend.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.BackendMessageReceiveEvent;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class BackendMessageReceiveListener {

    final TransportService<WebSocket> transportService;

    @Inject
    public BackendMessageReceiveListener(TransportService<WebSocket> transportService) {
        this.transportService = transportService;
    }


    @OmniSubscribe(priority = 99)
    public void onMessageReceive(BackendMessageReceiveEvent event) {
        final WebSocket webSocket = event.webSocket();
        final ByteBuf byteBuf = event.data();

        transportService.onReceiveRaw(webSocket, byteBuf);
    }
}
