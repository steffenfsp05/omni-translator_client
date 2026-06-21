package org.pytenix.packets.impl;

import org.pytenix.packets.PacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class TranslationResultMapper extends PacketMapper<NetworkPackets.TranslationResult, TranslationResultMapper.ResultData> {


    public TranslationResultMapper() {
        super(NetworkPackets.TranslationResult.class, TranslationResultMapper.ResultData.class);
    }

    @Override
    public NetworkPackets.TranslationResult to(ResultData packet) {
        return NetworkPackets.TranslationResult.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setResult(packet.result())
                .build();
    }

    @Override
    public ResultData from(NetworkPackets.TranslationResult packet) {
        return new ResultData(new UUID(packet.getRequestIdMostSig(),packet.getRequestIdLeastSig()),packet.getResult());
    }

    public record ResultData(UUID requestId, String result) { }
}
