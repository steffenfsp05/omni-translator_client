package org.pytenix.socket.inject;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.omni.packets.PacketRegistrar;
import org.omni.transport.TransportConnector;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.*;
import org.pytenix.socket.ExternPacketRegistrar;
import org.pytenix.socket.SocketTransportSender;
import org.pytenix.socket.endpoint.*;
import org.pytenix.socket.listener.*;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.service.impl.DefaultPacketService;

import java.net.http.WebSocket;

@Singleton
public class SocketModule extends AbstractModule {

    private final String backendRemoteAddress;

    public SocketModule(String backendRemoteAddress) {
        this.backendRemoteAddress = backendRemoteAddress;
    }

    @Override
    protected void configure() {

        bind(String.class).annotatedWith(Names.named("backendRemoteAddress")).toInstance(backendRemoteAddress);
        bind(WebSocketService.class).asEagerSingleton();
        bind(TransportConnector.class).to(WebSocketService.class).in(Scopes.SINGLETON);


        bind(new TypeLiteral<PacketRegistrar<WebSocket>>() {
        }).to(ExternPacketRegistrar.class).in(Scopes.SINGLETON);
        bind(TransportSender.class).to(SocketTransportSender.class).in(Scopes.SINGLETON);

        bind(GeoEndpoint.class).to(GeoSocketEndpoint.class).in(Scopes.SINGLETON);
        bind(ServerConfigurationEndpoint.class).to(ConfigurationSocketEndpoint.class).in(Scopes.SINGLETON);
        bind(ProfileEndpoint.class).to(ProfileSocketEndpoint.class).in(Scopes.SINGLETON);
        bind(TranslationEndpoint.class).to(TranslationSocketEndpoint.class).in(Scopes.SINGLETON);
        bind(DataExportEndpoint.class).to(DataExportSocketEndpoint.class).in(Scopes.SINGLETON);


        Multibinder<Object> omniListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("omniListeners"));
        omniListeners.addBinding().to(BackendCloseListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendConnectListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendMessageReceiveListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendConfigUpdateListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(BackendCacheInvalidationListener.class).in(Scopes.SINGLETON);


    }


    @Provides
    @Singleton
    public TransportService<WebSocket> provideExternalTransportService(
            PacketRegistrar<WebSocket> packetRegistrar,
            Provider<WebSocketService> webSocketServiceProvider) {

        final TransportService<WebSocket> transportService = TransportService.<WebSocket>builder()
                .packetService(new DefaultPacketService<>())
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(500)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(50000)
                        .build())
                .encryptionEnabled(false)
                .networkSender((ws, buf) -> webSocketServiceProvider.get().sendToWebSocket(ws, buf))
                .build();

        packetRegistrar.register(transportService);
        return transportService;
    }


}
