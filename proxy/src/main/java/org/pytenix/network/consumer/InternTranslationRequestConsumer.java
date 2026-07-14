package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.proto.generated.Protobuf;
import org.pytenix.backend.endpoint.TranslationSocketEndpoint;
import org.transport.service.PacketContext;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class InternTranslationRequestConsumer extends MappedPacketReceiveConsumer<RegisteredServer, Protobuf.TranslationRequest, TranslationRequestData> {

    private final CaffeineCacheProvider<String, String> caffeineCacheProvider;
    private final TranslationSocketEndpoint translationSocketEndpoint;

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Translation-API-Worker");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    public InternTranslationRequestConsumer(
            CaffeineCacheProvider<String, String> caffeineCacheProvider,
            TranslationSocketEndpoint translationSocketEndpoint
    ) {
        this.caffeineCacheProvider = caffeineCacheProvider;
        this.translationSocketEndpoint = translationSocketEndpoint;
    }

    private String generateKey(String text, String lang) {
        return text + ":" + lang;
    }

    @Override
    public void handle(PacketContext<RegisteredServer> context, TranslationRequestData requestData) {

        UUID id = requestData.requestId();
        String text = requestData.text();
        String lang = requestData.targetLanguage();
        String cacheKey = generateKey(text, lang);

        String cached = caffeineCacheProvider.get(cacheKey);

        if (cached != null) {
            reply(context, PacketRegistry.TRANSLATION_RESULT, new TranslationResultData(id, cached));
        } else {
            translationSocketEndpoint
                    .sendTranslationRequest(id, text, lang, requestData.module())
                    .thenAcceptAsync(translatedText -> {
                        String finalString = (isSuccessfull(translatedText) && !translatedText.equals(text)) ? translatedText : text;

                        reply(context, PacketRegistry.TRANSLATION_RESULT, new TranslationResultData(id, finalString));

                        caffeineCacheProvider.set(cacheKey, finalString);
                    }, executor);
        }
    }

    public boolean isSuccessfull(String string) {
        return string != null && !string.equalsIgnoreCase("TIMEOUT") && !string.startsWith("ERROR");
    }
}