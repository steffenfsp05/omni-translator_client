package org.pytenix.backend.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.proto.generated.Protobuf;
import org.pytenix.backend.endpoint.TranslationSocketEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendServerConfigConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.ServerConfiguration, ServerConfiguration> {

    private final TranslationSocketEndpoint translationSocketEndpoint;

    @Inject
    public BackendServerConfigConsumer(TranslationSocketEndpoint translationSocketEndpoint) {
        this.translationSocketEndpoint = translationSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, ServerConfiguration javaPacket) {
        translationSocketEndpoint.handleConfigUpdate(javaPacket);
    }
}
