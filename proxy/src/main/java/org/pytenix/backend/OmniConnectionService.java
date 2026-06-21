package org.pytenix.backend;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.proto.generated.NetworkPackets;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OmniConnectionService {

    public final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final String url;
    private final String apiKey;

    private final HttpClient httpClient;
    private WebSocket webSocket;

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ProxyServer proxyServer;
    private final TranslatorPlugin translatorPlugin;

    private RestfulService restfulService;
    private GeoService geoService;

    private final TransportService<WebSocket> transportService;

    public OmniConnectionService(TranslatorPlugin translatorPlugin, String apiKey, ProxyServer proxyServer) {
        this.translatorPlugin = translatorPlugin;
        this.proxyServer = proxyServer;
        this.apiKey = apiKey;
        this.url = "ws://" + translatorPlugin.getRemoteAddress() + "/ws/omni";

        this.httpClient = HttpClient.newBuilder()
                .executor(Executors.newCachedThreadPool())
                .build();

        this.transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(
                        TransportOptions.builder()
                                .batchingEnabled(true)
                                .maxBatchSize(100)
                                .batchingIntervalMs(5)
                                .maxPayloadSize(20000)
                                .build()
                )
                .encryptionEnabled(false)
                .networkSender(this::sendToWebSocket)
                .build();
    }

    public void setServices(RestfulService restfulService, GeoService geoService) {
        this.restfulService = restfulService;
        this.geoService = geoService;
        registerPackets();
    }

    private void registerPackets() {

        // Config Update registrieren
        transportService.registerPacket(PacketRegistry.SERVER_CONFIG, (ctx, protoConfig) -> {
            if (restfulService != null) {

                ServerConfiguration config = translatorPlugin.getTranslatorService().getServerConfigMapper().from(protoConfig);
                restfulService.handleConfigUpdate(config);
            }
        });

        transportService.registerPacket(PacketRegistry.TRANSLATION_RESULT, (ctx, packet) -> {
            if (restfulService != null) restfulService.handleTranslationResult(packet);
        });

        transportService.registerPacket(PacketRegistry.GEO_RESULT, (ctx, packet) -> {
            if (geoService != null) geoService.handleGeoResult(packet);
        });


        transportService.registerPacket(PacketRegistry.GEO_REQUEST,(webSocketPacketContext, geoRequestPacket) -> {});
        transportService.registerPacket(PacketRegistry.TRANSLATION_REQUEST,(webSocketPacketContext, translationRequest) -> {});
    }

    public void connect() {
        httpClient.newWebSocketBuilder()
                .header("X-API-KEY", apiKey)
                .buildAsync(URI.create(url), new WebSocketListener())
                .whenCompleteAsync((ws, ex) -> {
                    if (ex == null) {
                        this.webSocket = ws;
                        this.reconnectAttempts.set(0);
                        System.out.println("[OmniTranslator] ✅ Erfolgreich mit dem Dispatcher verbunden!");
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

        if (errorMsg.contains("401") || errorMsg.contains("Unauthorized") || errorMsg.contains("WebSocketHandshakeException")) {
            System.err.println("============================================");
            System.err.println("[OmniTranslator] ❌ ERROR: Invalid License!");
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
            isConnected.set(true);
            transportService.connect(webSocket);
            transportService.ready(webSocket);

            if (restfulService != null) {
                restfulService.sendTranslationRequest(java.util.UUID.randomUUID(), "This is a Test!", "de_de", "live_chat");
            }
            WebSocket.Listener.super.onOpen(webSocket);
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