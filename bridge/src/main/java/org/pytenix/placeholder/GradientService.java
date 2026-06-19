package org.pytenix.placeholder;

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

public interface GradientService {


    ExtractionResult stripAndAnalyze(String input);

    String restoreGradients(UUID uuid, String translatedText);

    void cacheGradient(UUID uuid, Map<String, GradientData> gradients);

    void invalidCachedGradient(UUID uuid);

    @Nullable
    Map<String, GradientService.GradientData> getCachedGradient(UUID uuid);

    record ExtractionResult(String cleanText, Map<String, GradientData> gradients) {
    }

    record GradientData(Color startColor, Color endColor, boolean bold, boolean italic) {
    }
}