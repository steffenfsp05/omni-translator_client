package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.omni.config.ConfigurationFile;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.packets.PacketRegistry;
import org.omni.packets.registry.PacketRegistrar;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.consumer.BackendGeoResultConsumer;
import org.pytenix.backend.consumer.BackendProfileResultConsumer;
import org.pytenix.backend.consumer.BackendServerConfigConsumer;
import org.pytenix.backend.consumer.BackendTranslationResultConsumer;
import org.pytenix.backend.socket.WebSocketService;
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

    private final TransportService<WebSocket> transportService;

    private final WebSocketService webSocketService;


    @Inject
    public OmniConnectionService(
            WebSocketService webSocketService,
            PacketRegistrar<WebSocket> packetRegistrar) {

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!!");



        this.webSocketService = webSocketService;


        this.transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(500)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(50000)
                        .build())
                .encryptionEnabled(false)
                .networkSender(webSocketService::sendToWebSocket)
                .build();

        packetRegistrar.register(transportService);

        System.out.println("OMNICONNECTIONSERVIUCE INIT!!!!!!!! REGUSTERED PACKEST");
    }

    public void connect()
    {
        webSocketService.connect();
    }

    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, MessageLite packet) {

        if(webSocketService == null) return;
        if(!webSocketService.getConnectionStatus().get()) return;

        transportService.send(webSocketService.getWebSocket(), packetDefinition.id(), packet);
    }


    public void shutdown() {
        if (transportService != null) transportService.close();
    }

}