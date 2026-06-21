package org.pytenix.packets.impl;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class TranslationRequestMapperAbstract extends AbstractPacketMapper<NetworkPackets.TranslationRequest, TranslationRequestMapperAbstract.RequestData> {

    private static final Map<NetworkPackets.Module, ServerConfiguration.Module> MODULE_MAP = new EnumMap<>(NetworkPackets.Module.class);

    static {
        for (NetworkPackets.Module protoMod : NetworkPackets.Module.values()) {
            if (protoMod == NetworkPackets.Module.MODULE_UNKNOWN || protoMod == NetworkPackets.Module.UNRECOGNIZED) continue;

            String javaName = protoMod.name().replace("MODULE_", "");
            try {
                MODULE_MAP.put(protoMod, ServerConfiguration.Module.valueOf(javaName));
            } catch (IllegalArgumentException e) {
                System.err.println("Modul gefunden, aber nicht in Java definiert: " + javaName);
            }
        }
    }

    public TranslationRequestMapperAbstract() {
        super(NetworkPackets.TranslationRequest.class, RequestData.class);
    }

    @Override
    public NetworkPackets.TranslationRequest to(RequestData packet) {
        NetworkPackets.Module protoModule = NetworkPackets.Module.valueOf("MODULE_" + packet.module().name());

        return NetworkPackets.TranslationRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setText(packet.text())
                .setTargetLang(packet.targetLanguage())
                .setModule(protoModule)
                .build();
    }

    @Override
    public RequestData from(NetworkPackets.TranslationRequest packet) {
        UUID requestId = new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig());

        ServerConfiguration.Module javaModule = MODULE_MAP.getOrDefault(packet.getModule(), ServerConfiguration.Module.LIVE_CHAT);

        return new RequestData(requestId, packet.getText(), packet.getTargetLang(), javaModule);
    }

    public record RequestData(UUID requestId, String text, String targetLanguage, ServerConfiguration.Module module) {}
}