package org.pytenix.backend.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.TranslationResultData;
import org.omni.proto.generated.Protobuf;
import org.pytenix.backend.TranslationSocketEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendTranslationResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.TranslationResult, TranslationResultData> {

    private final TranslationSocketEndpoint translationSocketEndpoint;

    @Inject
    public BackendTranslationResultConsumer(TranslationSocketEndpoint translationSocketEndpoint) {
        this.translationSocketEndpoint = translationSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, TranslationResultData javaPacket) {
        translationSocketEndpoint.handleTranslationResult(javaPacket);
    }
}