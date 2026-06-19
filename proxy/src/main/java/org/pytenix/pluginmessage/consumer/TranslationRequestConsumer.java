package org.pytenix.pluginmessage.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

import java.util.UUID;
import java.util.concurrent.Executor;

public class TranslationRequestConsumer implements PacketReceiveConsumer<RegisteredServer, NetworkPackets.TranslationRequest> {

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
    public void accept(PacketContext<RegisteredServer> context, NetworkPackets.TranslationRequest translationRequest) {


        UUID id = UuidUtil.fromByteString(translationRequest.getRequestId());
        String text = translationRequest.getText();
        String lang = translationRequest.getTargetLang();

        String cacheKey = generateKey(text, lang);

        String cached = translatorPlugin.getCaffeineCache().get(cacheKey);

        if (cached != null) {
            context.reply(PacketRegistry.TRANSLATION_RESULT,
                    NetworkPackets.TranslationResult.newBuilder()
                            .setRequestId(translationRequest.getRequestId())
                            .setResult(cached)
                            .build());
        } else {
            translatorPlugin.getRestfulService()
                    .sendTranslationRequest(id, text, lang, translationRequest.getModule())
                    .thenAcceptAsync(translatedText -> {
                        String finalString = (isSuccessfull(translatedText) && !translatedText.equals(text)) ? translatedText : text;

                        context.reply(PacketRegistry.TRANSLATION_RESULT,
                                NetworkPackets.TranslationResult.newBuilder()
                                        .setRequestId(translationRequest.getRequestId())
                                        .setResult(finalString)
                                        .build());
                        translatorPlugin.getCaffeineCache().set(cacheKey, finalString);

                    }, executor);
        }


    }

    public boolean isSuccessfull(String string) {
        return string != null && !string.equalsIgnoreCase("TIMEOUT") && !string.startsWith("ERROR");
    }
}
