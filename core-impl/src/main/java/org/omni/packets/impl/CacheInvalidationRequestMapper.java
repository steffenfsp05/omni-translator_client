package org.omni.packets.impl;

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import org.omni.entity.TranslationModule;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
@Singleton
public class CacheInvalidationRequestMapper extends AbstractPacketMapper<Protobuf.CacheInvalidationRequest, CacheInvalidationRequest> {


    private static final Map<Protobuf.Module, TranslationModule> MODULE_MAP = new EnumMap<>(Protobuf.Module.class);
    private static final Map<TranslationModule, Protobuf.Module> REVERSE_MODULE_MAP = new EnumMap<>(TranslationModule.class);

    static {
        for (Protobuf.Module protoMod : Protobuf.Module.values()) {
            if (protoMod == Protobuf.Module.MODULE_UNKNOWN || protoMod == Protobuf.Module.UNRECOGNIZED)
                continue;

            String javaName = protoMod.name().replace("MODULE_", "");
            try {
                TranslationModule javaModule = TranslationModule.valueOf(javaName);
                MODULE_MAP.put(protoMod, javaModule);
                REVERSE_MODULE_MAP.put(javaModule, protoMod);
            } catch (IllegalArgumentException e) {
                System.err.println("Modul gefunden, aber nicht in Java definiert: " + javaName);
            }
        }
    }

    public CacheInvalidationRequestMapper() {
        super(Protobuf.CacheInvalidationRequest.class, CacheInvalidationRequest.class);
    }

    @Override
    public Protobuf.CacheInvalidationRequest to(CacheInvalidationRequest packet) {
        Protobuf.CacheInvalidationRequest.Builder builder = Protobuf.CacheInvalidationRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits());

        if (packet.payload() instanceof CacheInvalidationRequest.Profile profile) {
            builder.setProfileInvalidation(
                    Protobuf.ProfileInvalidation.newBuilder()
                            .setAnalyticId(ByteString.copyFrom(profile.analyticId()))
                            .build()
            );
        } else if (packet.payload() instanceof CacheInvalidationRequest.Translation translation) {

            Protobuf.Module protoModule = REVERSE_MODULE_MAP.getOrDefault(
                    translation.translationModule(),
                    Protobuf.Module.MODULE_UNKNOWN
            );

            builder.setTranslationInvalidation(
                    Protobuf.TranslationInvalidation.newBuilder()
                            .setText(translation.text())
                            .setLanguage(translation.language())
                            .setModule(protoModule)
                            .build()
            );
        }

        return builder.build();
    }

    @Override
    public CacheInvalidationRequest from(Protobuf.CacheInvalidationRequest packet) {
        CacheInvalidationRequest.Payload payload = switch (packet.getPayloadCase()) {

            case PROFILE_INVALIDATION ->
                    new CacheInvalidationRequest.Profile(
                            packet.getProfileInvalidation().getAnalyticId().toByteArray()
                    );

            case TRANSLATION_INVALIDATION -> {
                TranslationModule javaModule = MODULE_MAP.getOrDefault(
                        packet.getTranslationInvalidation().getModule(),
                        TranslationModule.LIVE_CHAT
                );
                yield new CacheInvalidationRequest.Translation(
                        packet.getTranslationInvalidation().getText(),
                        packet.getTranslationInvalidation().getLanguage(),
                        javaModule
                );
            }

            case PAYLOAD_NOT_SET ->
                    throw new IllegalArgumentException("Invalidation Payload ist nicht gesetzt!");
        };

        return new CacheInvalidationRequest(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                payload
        );
    }
}