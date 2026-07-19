package org.omni.placeholder.pipeline.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.service.PlaceholderService;
import java.util.UUID;

@Singleton
public class PlaceholderProcessor implements TextProcessor {
    private final PlaceholderService placeholderService;

    @Inject
    public PlaceholderProcessor(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    @Override
    public String process(UUID id, String text) {
        return placeholderService.toPlaceholders(id, text);
    }

    @Override
    public String restore(UUID id, String text) {
        return placeholderService.fromPlaceholders(id, text);
    }
}