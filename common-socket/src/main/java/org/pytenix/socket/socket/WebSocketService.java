package org.pytenix.socket.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.omni.config.ConfigurationFile;
import org.omni.transport.TransportConnector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Setter
@Getter
public class WebSocketService implements AutoCloseable, TransportConnector {


    final AtomicBoolean connectionStatus = new AtomicBoolean(false);
    final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    final WebSocketListener webSocketListener;

    private final String apiKey;
    private final String url;

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private WebSocket webSocket;

    @Inject
    public WebSocketService(
            WebSocketListener webSocketListener,
            @Named("backendRemoteAddress") String backendRemoteAddress,
            ConfigurationFile configurationFile
    ) {

        this.webSocketListener = webSocketListener;

        this.url = backendRemoteAddress;
        this.apiKey = configurationFile.getLicenseKey();

        this.httpClient = HttpClient.newBuilder().executor(Executors.newCachedThreadPool()).build();
    }


    public void connect() {
        httpClient.newWebSocketBuilder()
                .header("X-API-KEY", apiKey)
                .buildAsync(URI.create(url), webSocketListener)
                .whenCompleteAsync((ws, ex) -> {
                    if (ex == null) {
                        this.reconnectAttempts.set(0);
                        System.out.println("[OmniTranslator]  Erfolgreich mit dem Dispatcher verbunden!");
                    } else {
                        handleConnectionError(ex);
                    }
                });
    }

    public void sendToWebSocket(WebSocket ws, ByteBuf nettyBuf) {
        if (ws != null && connectionStatus.get() && !ws.isOutputClosed()) {
            try {
                ByteBuffer nioBuffer = nettyBuf.nioBuffer();
                ws.sendBinary(nioBuffer, true);
            } finally {
                nettyBuf.release();
            }
        } else {
            nettyBuf.release();
        }
    }


    public void handleConnectionError(Throwable ex) {
        connectionStatus.set(false);
        String errorMsg = ex.toString();

        ex.printStackTrace();
        if (errorMsg.contains("401") || errorMsg.contains("Unauthorized") || errorMsg.contains("WebSocketHandshakeException")) {
            System.err.println("============================================");
            System.err.println("[OmniTranslator] ❌ ERROR: Invalid License! " + apiKey);
            System.err.println("[OmniTranslator] Connection will be permanently terminated.");
            System.err.println("============================================");
        } else {
            System.err.println("[OmniTranslator] ⚠️ Connection Error: " + ex.getMessage());
            scheduleReconnect();
        }
    }

    public void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        long waitTime = Math.min((long) Math.pow(4, attempts), 60);

        System.err.println("[OmniTranslator] Reconnect in " + waitTime + "s...");


        scheduler.schedule(this::connect, waitTime, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
