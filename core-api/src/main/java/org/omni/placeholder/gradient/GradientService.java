package org.omni.placeholder.gradient;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface GradientService {


    ExtractionResult stripAndAnalyze(String input);

    String restoreGradients(UUID uuid, String translatedText);

    void cacheGradient(UUID uuid, Map<String, GradientData> gradients);

    void invalidCachedGradient(UUID uuid);

    @Nullable
    Map<String, GradientData> getCachedGradient(UUID uuid);

}