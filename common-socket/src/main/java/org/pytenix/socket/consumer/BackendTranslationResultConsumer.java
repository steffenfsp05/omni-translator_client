package org.pytenix.socket.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.TranslationResultData;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.TranslationEndpoint;
import org.pytenix.socket.endpoint.TranslationSocketEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendTranslationResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.TranslationResult, TranslationResultData> {

    private final TranslationEndpoint translationSocketEndpoint;

    @Inject
    public BackendTranslationResultConsumer(TranslationEndpoint translationSocketEndpoint) {
        this.translationSocketEndpoint = translationSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, TranslationResultData javaPacket) {
        translationSocketEndpoint.handleIncoming(javaPacket);
    }
}