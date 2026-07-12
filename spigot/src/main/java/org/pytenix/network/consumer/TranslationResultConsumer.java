package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.TranslationResultData;
import org.omni.proto.generated.Protobuf;
import org.pytenix.network.service.TranslationRequestService;
import org.transport.service.PacketContext;

@Singleton
public class TranslationResultConsumer extends MappedPacketReceiveConsumer<String, Protobuf.TranslationResult, TranslationResultData> {

    final TranslationRequestService translationRequestService;

    @Inject
    public TranslationResultConsumer(TranslationRequestService translationRequestService) {
        this.translationRequestService = translationRequestService;
    }

    @Override
    public void handle(PacketContext<String> context, TranslationResultData resultData) {
        translationRequestService.completeRequest(
                resultData.requestId(),
                resultData.result()
        );
    }
}
