package org.pytenix;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.placeholder.GradientService;
import org.pytenix.placeholder.PlaceholderService;
import org.pytenix.placeholder.listener.ConfigUpdateListener;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
public abstract class TranslatorService {


    final GradientService gradientService;
    final PlaceholderService placeholderService;

    private final Cache<UUID, List<UUID>> cachedReferences = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();


    private final EventService eventService;

    @Setter
    private org.pytenix.entity.ServerConfiguration translationConfiguration;


    public TranslatorService() {

        this.placeholderService = new PlaceholderService(this);
        this.gradientService = new GradientService();

        this.eventService = new EventService();

        eventService.register(new ConfigUpdateListener(placeholderService));


    }


    protected abstract CompletableFuture<String> process(UUID id, String text, String targetLang, String module);


    public String preparePayload(UUID batchId, String text) {
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

    public CompletableFuture<String> processAndRestore(UUID batchId, String payload, String lang, String module, long started) {
        return process(batchId, payload, lang, module)
                .thenApplyAsync(s -> {
                    return handlePlaceholders(batchId, s); // Gradients & Farben zurück!
                });
    }

    public CompletableFuture<String> translate(String text, String lang, String module) {
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        long started = System.currentTimeMillis();
        UUID batchId = UUID.randomUUID();

        String prepared = preparePayload(batchId, text);
        return processAndRestore(batchId, prepared, lang, module, started);
    }

    public String handleGradient(UUID uuid, String text) {
        GradientService.ExtractionResult extractionResult = gradientService.stripAndAnalyze(text);
        if (extractionResult.gradients() != null) {
            gradientService.cachedGradients.put(uuid, extractionResult.gradients());
            return extractionResult.cleanText();
        }
        return text;
    }


    public NetworkPackets.ServerConfiguration convertConfigToProtobuf(ServerConfiguration javaConfig) {
        NetworkPackets.ServerConfiguration.Builder builder = NetworkPackets.ServerConfiguration.newBuilder();
        if (javaConfig.getModules() != null) builder.putAllModules(javaConfig.getModules());
        if (javaConfig.getBlacklistedWords() != null) builder.addAllWords(javaConfig.getBlacklistedWords());
        if (javaConfig.getDefaultLanguage() != null) builder.setDefaultLanguage(javaConfig.getDefaultLanguage());
        return builder.build();
    }

    public ServerConfiguration convertConfigToNormal(NetworkPackets.ServerConfiguration serverConfiguration) {
        org.pytenix.entity.ServerConfiguration update = new org.pytenix.entity.ServerConfiguration();

        update.setModules(new HashMap<>(serverConfiguration.getModulesMap()));
        update.setBlacklistedWords(new HashSet<>(serverConfiguration.getWordsList()));
        update.setDefaultLanguage(serverConfiguration.getDefaultLanguage());

        return update;
    }

    public String handlePlaceholders(UUID uuid, String result) {

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
                @Nullable Map<String, GradientService.GradientData> gradientInfo = gradientService.cachedGradients.getIfPresent(lineUuid);
                if (gradientInfo != null) {
                    currentLine = gradientService.restoreGradients(lineUuid, currentLine);
                    gradientService.cachedGradients.invalidate(lineUuid);
                }
            }

            finalLines.add(currentLine);
        }


        cachedReferences.invalidate(uuid);


        return String.join("\n", finalLines);
    }

}
