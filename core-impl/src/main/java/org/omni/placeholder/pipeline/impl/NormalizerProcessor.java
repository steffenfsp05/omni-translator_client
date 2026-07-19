package org.omni.placeholder.pipeline.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.normalizer.PlaceholderNormalizer;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.UUID;

@Singleton
public class NormalizerProcessor implements TextProcessor {
    private final PlaceholderNormalizer normalizer;

    @Inject
    public NormalizerProcessor(PlaceholderNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public String process(UUID id, String text) {
        return normalizer.normalizeText(id, text);
    }

    @Override
    public String restore(UUID id, String text) {
        return normalizer.denormalizeText(id, text);
    }
}
