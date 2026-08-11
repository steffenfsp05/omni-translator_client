package org.omni.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.EventService; // Angenommener Name deines Event-Managers
import org.omni.event.impl.DefaultEventService;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.TranslationPipeline;
import org.omni.placeholder.processor.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Singleton
public class DefaultTranslationPipeline implements TranslationPipeline {
    private final List<TextProcessor> processors;
    private final List<TextProcessor> reversedProcessors;

    @Inject
    public DefaultTranslationPipeline(EventService eventService, List<TextProcessor> list) {
        this.processors = List.copyOf(list);

        for (TextProcessor processor : processors) {
            eventService.register(processor);
        }

        List<TextProcessor> rev = new ArrayList<>(list);
        Collections.reverse(rev);
        this.reversedProcessors = List.copyOf(rev);
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
        for (TextProcessor p : reversedProcessors) {
            current = p.restore(id, current);
        }
        return current;
    }
}