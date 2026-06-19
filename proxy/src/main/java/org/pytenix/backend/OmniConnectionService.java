package org.pytenix.backend;

import com.velocitypowered.api.proxy.ProxyServer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.FastByteArrayOutputStream;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OmniConnectionService {

    public final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final String url;
    private final String apiKey;

    private final HttpClient httpClient;
    private final ExecutorService parsingExecutor;

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ProxyServer proxyServer;
    private final TranslatorPlugin translatorPlugin;
    private WebSocket webSocket;
    private RestfulService restfulService;
    private GeoService geoService;

    public OmniConnectionService(TranslatorPlugin translatorPlugin, String apiKey, ProxyServer proxyServer) {
        this.translatorPlugin = translatorPlugin;
        this.proxyServer = proxyServer;
        this.apiKey = apiKey;

        this.url = "ws://" + translatorPlugin.getRemoteAddress() + "/ws/omni";

        this.httpClient = HttpClient.newBuilder()
                .executor(Executors.newCachedThreadPool())
                .build();

        this.parsingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    public void setServices(RestfulService restfulService, GeoService geoService) {
        this.restfulService = restfulService;
        this.geoService = geoService;
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

    public CompletableFuture<WebSocket> sendPacket(NetworkPackets.PacketWrapper wrapper) {
        if (webSocket != null && isConnected.get() && !webSocket.isOutputClosed()) {
            return webSocket.sendBinary(ByteBuffer.wrap(wrapper.toByteArray()), true);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("WebSocket not connected"));
    }

    private void handleConnectionError(Throwable ex) {
        this.isConnected.set(false);
        String errorMsg = ex.toString();

        ex.printStackTrace();
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

    private class WebSocketListener implements WebSocket.Listener {
        private final FastByteArrayOutputStream buffer = new FastByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket webSocket) {
            isConnected.set(true);
            // Initiale Test-Nachricht
            if (restfulService != null) {
                restfulService.sendTranslationRequest(java.util.UUID.randomUUID(), "This is a Test!", "de_de", "live_chat");
            }
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            buffer.write(data);
            if (last) {
                try {
                    InputStream inputStream = buffer.toInputStream();
                    NetworkPackets.PacketWrapper wrapper = NetworkPackets.PacketWrapper.parseFrom(inputStream);
                    buffer.reset();
                    parsingExecutor.submit(() -> processPacket(wrapper));
                } catch (Exception e) {
                    e.printStackTrace();
                    buffer.reset();
                }
            }
            webSocket.request(1);
            return null;
        }

        private void processPacket(NetworkPackets.PacketWrapper wrapper) {
            try {
                switch (wrapper.getPayloadCase()) {
                    case BATCH_RESULT:
                        if (restfulService != null) restfulService.handleTranslationResult(wrapper.getBatchResult());
                        break;

                    case GEO_BATCH_RESULT:
                        if (geoService != null) geoService.handleGeoResult(wrapper.getGeoBatchResult());
                        break;

                    case CONFIG:
                    case CONFIG_REQUEST:
                        if (restfulService != null) {
                            NetworkPackets.ServerConfiguration protoConfig = wrapper.getConfig();
                            ServerConfiguration config = new ServerConfiguration();
                            config.setModules(new HashMap<>(protoConfig.getModulesMap()));
                            config.setLicenseKey(protoConfig.getLicenseKey());
                            config.setDefaultLanguage(protoConfig.getDefaultLanguage());
                            config.setBlacklistedWords(new HashSet<>(protoConfig.getWordsList()));
                            restfulService.handleConfigUpdate(config);
                        }
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            isConnected.set(false);
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
        }
    }
}