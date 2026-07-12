package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.TranslationRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class TranslationRequestMapper extends AbstractPacketMapper<Protobuf.TranslationRequest, TranslationRequestData> {

    private static final Map<Protobuf.Module, TranslationModule> MODULE_MAP = new EnumMap<>(Protobuf.Module.class);

    static {
        for (Protobuf.Module protoMod : Protobuf.Module.values()) {
            if (protoMod == Protobuf.Module.MODULE_UNKNOWN || protoMod == Protobuf.Module.UNRECOGNIZED)
                continue;

            String javaName = protoMod.name().replace("MODULE_", "");
            try {
                MODULE_MAP.put(protoMod, TranslationModule.getModule(javaName));
            } catch (IllegalArgumentException e) {
                System.err.println("Modul gefunden, aber nicht in Java definiert: " + javaName);
            }
        }
    }

    public TranslationRequestMapper() {
        super(Protobuf.TranslationRequest.class, TranslationRequestData.class);
    }

    @Override
    public Protobuf.TranslationRequest to(TranslationRequestData packet) {
        Protobuf.Module protoModule = Protobuf.Module.valueOf("MODULE_" + packet.module().name());

        return Protobuf.TranslationRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setText(packet.text())
                .setTargetLang(packet.targetLanguage())
                .setModule(protoModule)
                .build();
    }

    @Override
    public TranslationRequestData from(Protobuf.TranslationRequest packet) {
        UUID requestId = new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig());

        TranslationModule javaModule = MODULE_MAP.getOrDefault(packet.getModule(), TranslationModule.LIVE_CHAT);

        return new TranslationRequestData(requestId, packet.getText(), packet.getTargetLang(), javaModule);
    }


}