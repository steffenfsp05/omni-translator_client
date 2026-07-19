package org.omni.placeholder.pipeline.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.protector.ProtectionResult;
import org.omni.placeholder.protector.WordProtector;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class WordProtectorProcessor implements TextProcessor {

    private final WordProtector protector;
    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Inject
    public WordProtectorProcessor(WordProtector protector) {
        this.protector = protector;
    }

    @Override
    public String process(UUID id, String text) {
        ProtectionResult result = protector.protect(text);
        if (!result.replacements().isEmpty()) {
            cache.put(id, result.replacements());
            return result.maskedText();
        }
        return text;
    }

    @Override
    public String restore(UUID id, String text) {
        Map<String, String> replacements = cache.getIfPresent(id);
        if (replacements != null) {
            cache.invalidate(id);
            return protector.restore(text, replacements);
        }
        return text;
    }
}