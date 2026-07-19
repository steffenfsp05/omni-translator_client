package org.pytenix.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.PacketRegistrar;
import org.omni.packets.PacketRegistry;
import org.pytenix.socket.consumer.*;
import org.transport.TransportService;

import java.net.http.WebSocket;

@Singleton
public class ExternPacketRegistrar implements PacketRegistrar<WebSocket> {

    private final BackendServerConfigConsumer backendServerConfigConsumer;
    private final BackendTranslationResultConsumer backendTranslationResultConsumer;
    private final BackendGeoResultConsumer backendGeoResultConsumer;
    private final BackendProfileResultConsumer backendProfileResultConsumer;
    private final BackendCacheInvalidationConsumer cacheInvalidationConsumer;

    @Inject
    public ExternPacketRegistrar(
            BackendServerConfigConsumer backendServerConfigConsumer,
            BackendTranslationResultConsumer backendTranslationResultConsumer,
            BackendGeoResultConsumer backendGeoResultConsumer,
            BackendProfileResultConsumer backendProfileResultConsumer,
            BackendCacheInvalidationConsumer cacheInvalidationConsumer
    ) {

        this.backendServerConfigConsumer = backendServerConfigConsumer;
        this.backendTranslationResultConsumer = backendTranslationResultConsumer;
        this.backendGeoResultConsumer = backendGeoResultConsumer;
        this.backendProfileResultConsumer = backendProfileResultConsumer;
        this.cacheInvalidationConsumer = cacheInvalidationConsumer;
    }

    @Override
    public void register(TransportService<WebSocket> transport) {
        transport.registerPacket(PacketRegistry.SERVER_CONFIG, backendServerConfigConsumer);
        transport.registerPacket(PacketRegistry.TRANSLATION_RESULT, backendTranslationResultConsumer);
        transport.registerPacket(PacketRegistry.GEO_RESULT, backendGeoResultConsumer);
        transport.registerPacket(PacketRegistry.PROFILE, backendProfileResultConsumer);
        transport.registerPacket(PacketRegistry.CACHE_INVALIDATION, cacheInvalidationConsumer);

        transport.registerPacket(PacketRegistry.GEO_REQUEST, (ctx, req) -> {
        });
        transport.registerPacket(PacketRegistry.TRANSLATION_REQUEST, (ctx, req) -> {
        });
        transport.registerPacket(PacketRegistry.PROFILE_UPDATE_EXTERN, (ctx, req) -> {
        });
    }


}