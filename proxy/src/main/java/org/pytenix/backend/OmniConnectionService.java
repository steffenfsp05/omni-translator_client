package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.omni.config.ConfigurationFile;
import org.omni.packets.PacketRegistry;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.consumer.BackendGeoResultConsumer;
import org.pytenix.backend.consumer.BackendProfileResultConsumer;
import org.pytenix.backend.consumer.BackendServerConfigConsumer;
import org.pytenix.backend.consumer.BackendTranslationResultConsumer;
import org.pytenix.util.FastByteArrayOutputStream;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.service.impl.DefaultPacketService;
import org.transport.service.impl.PacketDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class OmniConnectionService {

    public final AtomicBoolean isConnected = new AtomicBoolean(false);
    @Getter
    private final String apiKey;
    private final String url;
    private final HttpClient httpClient;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ProxyServer proxyServer;
    private final TranslatorPlugin translatorPlugin;
    private final TransportService<WebSocket> transportService;

    private final TranslationSocketEndpoint translationSocketEndpoint;

    private final BackendServerConfigConsumer backendServerConfigConsumer;
    private final BackendTranslationResultConsumer backendTranslationResultConsumer;
    private final BackendGeoResultConsumer backendGeoResultConsumer;
    private final BackendProfileResultConsumer backendProfileResultConsumer;

    private WebSocket webSocket;

    @Inject
    public OmniConnectionService(
            TranslatorPlugin translatorPlugin,
            ConfigurationFile config,
            ProxyServer proxyServer,
            TranslationSocketEndpoint translationSocketEndpoint,
            BackendServerConfigConsumer backendServerConfigConsumer,
            BackendTranslationResultConsumer backendTranslationResultConsumer,
            BackendGeoResultConsumer backendGeoResultConsumer,
            BackendProfileResultConsumer backendProfileResultConsumer) {

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!!");

        this.translatorPlugin = translatorPlugin;
        this.proxyServer = proxyServer;
        this.apiKey = config.getLicenseKey();

        this.translationSocketEndpoint = translationSocketEndpoint;

        this.backendServerConfigConsumer = backendServerConfigConsumer;
        this.backendTranslationResultConsumer = backendTranslationResultConsumer;
        this.backendGeoResultConsumer = backendGeoResultConsumer;
        this.backendProfileResultConsumer = backendProfileResultConsumer;

        this.url = "ws://192.168.178.121:8083/ws/omni";

        this.httpClient = HttpClient.newBuilder().executor(Executors.newCachedThreadPool()).build();

        this.transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(500)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(50000)
                        .build())
                .encryptionEnabled(false)
                .networkSender(this::sendToWebSocket)
                .build();

        registerPackets();
        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!! REGUSTERED PACKEST");
    }

    private void registerPackets() {
        transportService.registerPacket(PacketRegistry.SERVER_CONFIG, backendServerConfigConsumer);
        transportService.registerPacket(PacketRegistry.TRANSLATION_RESULT, backendTranslationResultConsumer);
        transportService.registerPacket(PacketRegistry.GEO_RESULT, backendGeoResultConsumer);
        transportService.registerPacket(PacketRegistry.PROFILE, backendProfileResultConsumer);

        transportService.registerPacket(PacketRegistry.GEO_REQUEST, (ctx, req) -> {
        });
        transportService.registerPacket(PacketRegistry.TRANSLATION_REQUEST, (ctx, req) -> {
        });
        transportService.registerPacket(PacketRegistry.PROFILE_UPDATE_EXTERN, (ctx, req) -> {
        });
    }

    public void connect() {
        System.out.println("CONNECTING!!!!!!! ");
        httpClient.newWebSocketBuilder()
                .header("X-API-KEY", apiKey)
                .buildAsync(URI.create(url), new WebSocketListener())
                .whenCompleteAsync((ws, ex) -> {
                    if (ex == null) {
                        this.reconnectAttempts.set(0);
                        System.out.println("[OmniTranslator]  Erfolgreich mit dem Dispatcher verbunden!");
                    } else {
                        handleConnectionError(ex);
                    }
                });
    }

    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, MessageLite packet) {
        if (webSocket != null && isConnected.get()) {
            transportService.send(webSocket, packetDefinition.id(), packet);
        }
    }

    private void sendToWebSocket(WebSocket ws, ByteBuf nettyBuf) {
        if (ws != null && isConnected.get() && !ws.isOutputClosed()) {
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

    private void handleConnectionError(Throwable ex) {
        this.isConnected.set(false);
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

    private void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        long waitTime = Math.min((long) Math.pow(4, attempts), 60);

        System.err.println("[OmniTranslator] Reconnect in " + waitTime + "s...");
        proxyServer.getScheduler().buildTask(translatorPlugin, this::connect)
                .delay(waitTime, TimeUnit.SECONDS)
                .schedule();
    }

    public void shutdown() {
        if (transportService != null) transportService.close();
    }

    private class WebSocketListener implements WebSocket.Listener {
        private final FastByteArrayOutputStream buffer = new FastByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket webSocket) {
            OmniConnectionService.this.webSocket = webSocket;
            isConnected.set(true);
            transportService.connect(webSocket);
            transportService.ready(webSocket);
            WebSocket.Listener.super.onOpen(webSocket);

            if (translationSocketEndpoint != null) {
                System.out.println("Sending: This is a Test!");
                translationSocketEndpoint.sendTranslationRequest(java.util.UUID.randomUUID(), "This is a Test!", "de_de", "live_chat");
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            try {
                buffer.write(chunk);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data.position(data.limit());

            if (last) {
                byte[] fullPayload = buffer.toByteArray();
                buffer.reset();
                ByteBuf nettyBuf = Unpooled.wrappedBuffer(fullPayload);
                transportService.onReceiveRaw(webSocket, nettyBuf);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            isConnected.set(false);
            transportService.disconnect(webSocket);
            if (statusCode == 1008) {
                System.err.println("[OmniTranslator] FATAL: Verbindung wegen Lizenzfehlern geschlossen. Kein Reconnect.");
                return null;
            }
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            isConnected.set(false);
            transportService.disconnect(webSocket);
        }
    }
}