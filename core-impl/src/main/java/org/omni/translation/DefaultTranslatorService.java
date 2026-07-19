package org.omni.translation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.ServerConsentMode;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.packets.data.TranslationRequestData;
import org.omni.placeholder.listener.ConfigUpdateListener;
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;
import org.omni.placeholder.service.PlaceholderService;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.omni.transport.endpoint.TranslationEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Singleton
public class DefaultTranslatorService implements TranslatorService {

    final TranslationEndpoint translationEndpoint;
    final DefaultTranslationPipeline pipeline;
    final EventService eventService;
    final PlayerLocaleProcessor playerLocaleProcessor;
    final ProfileEndpoint profileEndpoint;

    private final Cache<UUID, List<UUID>> cachedReferences = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Setter
    private ServerConfiguration translationConfiguration;

    @Inject
    public DefaultTranslatorService(
            TranslationEndpoint translationEndpoint,
            DefaultTranslationPipeline pipeline,
            ProfileEndpoint profileEndpoint,
            EventService eventService,
            PlayerLocaleProcessor playerLocaleProcessor,
            PlaceholderService placeholderService) {

        this.translationEndpoint = translationEndpoint;
        this.pipeline = pipeline;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.profileEndpoint = profileEndpoint;
        this.eventService = eventService;


        eventService.register(new ConfigUpdateListener(placeholderService));
    }

    public CompletableFuture<String> translate(String text, String lang, TranslationModule module) {
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        UUID batchId = UUID.randomUUID();

        String prepared = preparePayload(batchId, text);
        return processAndRestore(batchId, prepared, lang, module);
    }

    @Override
    public CompletableFuture<Boolean> requiresTranslation(UUID playerUUID) {
        if (translationConfiguration == null || translationConfiguration.getDefaultLanguage() == null) {
            return CompletableFuture.completedFuture(true);
        }

        String playerLocale = playerLocaleProcessor.retrieveLocale(playerUUID);
        if (playerLocale != null && playerLocale.startsWith(translationConfiguration.getDefaultLanguage().toLowerCase())) {
            return CompletableFuture.completedFuture(false);
        }

        return profileEndpoint.sendRequest(playerUUID)
                .thenApply(profileData -> {
                    if (translationConfiguration.getConsentMode().equals(ServerConsentMode.AUTO_OPT) &&
                            profileData.consentType().equals(Protobuf.ConsentType.AUTO))
                        return true;

                    return !profileData.consentType().equals(Protobuf.ConsentType.DECLINED);
                });
    }

    public CompletableFuture<String> process(UUID id, String text, String targetLang, TranslationModule translationModule) {
        return translationEndpoint.sendRequest(new TranslationRequestData(
                id, text, targetLang, translationModule
        ));
    }

    private String preparePayload(UUID batchId, String text) {
        List<String> processedLines = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        List<UUID> lineUuids = new ArrayList<>();

        for (String line : lines) {
            UUID lineId = UUID.randomUUID();
            String maskedText = pipeline.prepare(lineId, line);
            processedLines.add(maskedText);
            lineUuids.add(lineId);
        }

        cachedReferences.put(batchId, lineUuids);
        return String.join("\n", processedLines);
    }

    public CompletableFuture<String> processAndRestore(UUID batchId, String payload, String lang, TranslationModule translationModule) {
        return process(batchId, payload, lang, translationModule)
                .thenApplyAsync(s -> restorePayload(batchId, s));
    }

    private String restorePayload(UUID batchId, String result) {
        List<UUID> lineIds = cachedReferences.getIfPresent(batchId);
        if (lineIds == null || lineIds.isEmpty()) return result;

        String[] translatedLines = result.split("\n", -1);
        List<String> finalLines = new ArrayList<>();

        for (int i = 0; i < lineIds.size(); i++) {
            UUID lineUuid = lineIds.get(i);
            String currentLine = (i < translatedLines.length) ? translatedLines[i] : "";

            finalLines.add(pipeline.restore(lineUuid, currentLine));
        }

        cachedReferences.invalidate(batchId);
        return String.join("\n", finalLines);
    }
}