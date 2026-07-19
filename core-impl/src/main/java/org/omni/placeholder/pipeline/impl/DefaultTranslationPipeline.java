package org.omni.placeholder.pipeline.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.TranslationPipeline;
import java.util.*;

@Singleton
public class DefaultTranslationPipeline implements TranslationPipeline {
    private final List<TextProcessor> processors;

    @Inject
    public DefaultTranslationPipeline(List<TextProcessor> processors) {
        this.processors = new ArrayList<>(processors);
    }

    @Override
    public String prepare(UUID id, String text) {
        String current = text;
        for (TextProcessor p : processors) {
            current = p.process(id, current);
        }
        return current;
    }

    @Override
    public String restore(UUID id, String text) {
        String current = text;
        List<TextProcessor> reversed = new ArrayList<>(processors);
        Collections.reverse(reversed);

        for (TextProcessor p : reversed) {
            current = p.restore(id, current);
        }
        return current;
    }
}