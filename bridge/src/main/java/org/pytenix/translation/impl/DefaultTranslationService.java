package org.pytenix.translation.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.Setter;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.event.EventService;
import org.pytenix.placeholder.GradientService;
import org.pytenix.placeholder.PlaceholderService;
import org.pytenix.placeholder.listener.ConfigUpdateListener;
import org.pytenix.translation.TranslationProcessor;
import org.pytenix.translation.TranslatorService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class DefaultTranslationService implements TranslatorService {


    final TranslationProcessor translationProcessor;
    final PlaceholderService placeholderService;
    final GradientService gradientService;
    final EventService eventService;


    private final Cache<UUID, List<UUID>> cachedReferences = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();


    @Setter
    private org.pytenix.entity.ServerConfiguration translationConfiguration;

    public DefaultTranslationService(
            TranslationProcessor translationProcessor,
            PlaceholderService placeholderService,
            GradientService gradientService,
            EventService eventService) {

        this.translationProcessor = translationProcessor;
        this.placeholderService = placeholderService;
        this.gradientService = gradientService;

        this.eventService = eventService;

        eventService.register(new ConfigUpdateListener(placeholderService));


    }



    public CompletableFuture<String> translate(String text, String lang, String module) {
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        UUID batchId = UUID.randomUUID();

        String prepared = preparePayload(batchId, text);
        return processAndRestore(batchId, prepared, lang, module);
    }

    public String handleGradient(UUID uuid, String text) {
        GradientService.ExtractionResult extractionResult = gradientService.stripAndAnalyze(text);
        if (extractionResult.gradients() != null) {
            gradientService.cacheGradient(uuid, extractionResult.gradients());
            return extractionResult.cleanText();
        }
        return text;
    }


    public CompletableFuture<String> process(UUID id, String text, String targetLang, String module) {
        return translationProcessor.endpointTranslation(id, text, targetLang, module);
    }


    private String preparePayload(UUID batchId, String text) {
        List<String> processedLines = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        List<UUID> lineUuids = new ArrayList<>();

        for (String line : lines) {
            UUID lineId = UUID.randomUUID();
            String cleanText = handleGradient(lineId, line); // Gradient raus!
            String maskedText = placeholderService.toPlaceholders(lineId, cleanText);
            processedLines.add(maskedText);
            lineUuids.add(lineId);
        }

        getCachedReferences().put(batchId, lineUuids);
        return String.join("\n", processedLines);
    }

    public CompletableFuture<String> processAndRestore(UUID batchId, String payload, String lang, String module) {
        return process(batchId, payload, lang, module)
                .thenApplyAsync(s -> handlePlaceholders(batchId, s));
    }



    private String handlePlaceholders(UUID uuid, String result) {

        List<UUID> lineIds = cachedReferences.getIfPresent(uuid);
        if (lineIds == null || lineIds.isEmpty())
            return result;


        //TODO MORGEN TESTEN

        String[] translatedLines = result.split("\n", -1);
        List<String> finalLines = new ArrayList<>();

        for (int i = 0; i < lineIds.size(); i++) {
            UUID lineUuid = lineIds.get(i);

            String currentLine = (i < translatedLines.length) ? translatedLines[i] : "";

            if (placeholderService != null) {
                currentLine = placeholderService.fromPlaceholders(lineUuid, currentLine);
            }

            if (gradientService != null) {

                Map<String, GradientService.GradientData> gradientInfo = gradientService.getCachedGradient(lineUuid);

                if (gradientInfo != null) {
                    currentLine = gradientService.restoreGradients(lineUuid, currentLine);
                    gradientService.invalidCachedGradient(lineUuid);
                }
            }

            finalLines.add(currentLine);
        }


        cachedReferences.invalidate(uuid);


        return String.join("\n", finalLines);
    }

}

