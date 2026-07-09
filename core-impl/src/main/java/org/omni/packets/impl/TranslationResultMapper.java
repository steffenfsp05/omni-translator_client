package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.TranslationResultData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

@Singleton
public class TranslationResultMapper extends AbstractPacketMapper<Protobuf.TranslationResult, TranslationResultData> {


    public TranslationResultMapper() {
        super(Protobuf.TranslationResult.class, TranslationResultData.class);
    }

    @Override
    public Protobuf.TranslationResult to(TranslationResultData packet) {
        return Protobuf.TranslationResult.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setResult(packet.result())
                .build();
    }

    @Override
    public TranslationResultData from(Protobuf.TranslationResult packet) {
        return new TranslationResultData(new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()), packet.getResult());
    }


}
