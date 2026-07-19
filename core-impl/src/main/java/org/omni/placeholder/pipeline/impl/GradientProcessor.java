package org.omni.placeholder.pipeline.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.gradient.ExtractionResult;
import org.omni.placeholder.gradient.GradientService;
import java.util.UUID;

@Singleton
public class GradientProcessor implements TextProcessor {
    private final GradientService gradientService;

    @Inject
    public GradientProcessor(GradientService gradientService) {
        this.gradientService = gradientService;
    }

    @Override
    public String process(UUID id, String text) {
        ExtractionResult result = gradientService.stripAndAnalyze(text);
        if (result != null && result.gradients() != null) {
            gradientService.cacheGradient(id, result.gradients());
            return result.cleanText();
        }
        return text;
    }

    @Override
    public String restore(UUID id, String text) {
        String restored = gradientService.restoreGradients(id, text);
        gradientService.invalidCachedGradient(id);
        return restored;
    }
}