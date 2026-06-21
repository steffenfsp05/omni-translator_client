package org.pytenix.network.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.TranslationRequestMapper;
import org.pytenix.packets.impl.TranslationResultMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

import java.util.UUID;
import java.util.concurrent.Executor;

public class TranslationRequestConsumer implements MappedPacketReceiveConsumer<RegisteredServer, NetworkPackets.TranslationRequest, TranslationRequestMapper.RequestData> {

    final TranslatorPlugin translatorPlugin;
    final Executor executor;

    public TranslationRequestConsumer(TranslatorPlugin translatorPlugin, Executor executor) {
        this.translatorPlugin = translatorPlugin;
        this.executor = executor;
    }


    private String generateKey(String text, String lang) {
        return text + ":" + lang;
    }

    @Override
    public void handle(PacketContext<RegisteredServer> context, TranslationRequestMapper.RequestData requestData) {


        UUID id = requestData.requestId();
        String text = requestData.text();
        String lang = requestData.targetLanguage();

        String cacheKey = generateKey(text, lang);

        String cached = translatorPlugin.getCaffeineCache().get(cacheKey);

        if (cached != null) {
            context.reply(PacketRegistry.TRANSLATION_RESULT,
                    PacketMapperRegistry.toProto(new TranslationResultMapper.ResultData(
                            id,
                            cached
                    )));
        } else {
            translatorPlugin.getRestfulService()
                    .sendTranslationRequest(id, text, lang, requestData.module().name())
                    .thenAcceptAsync(translatedText -> {
                        String finalString = (isSuccessfull(translatedText) && !translatedText.equals(text)) ? translatedText : text;

                        context.reply(PacketRegistry.TRANSLATION_RESULT,
                                PacketMapperRegistry.toProto(new TranslationResultMapper.ResultData(
                                        id,
                                        finalString
                                )));

                        translatorPlugin.getCaffeineCache().set(cacheKey, finalString);

                    }, executor);
        }


    }


    public boolean isSuccessfull(String string) {
        return string != null && !string.equalsIgnoreCase("TIMEOUT") && !string.startsWith("ERROR");
    }
}
