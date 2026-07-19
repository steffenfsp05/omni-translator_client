package org.omni.placeholder.pipeline.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.protector.PlayerNameProtector;
import org.omni.placeholder.protector.ProtectionResult;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class NameProtectorProcessor implements TextProcessor {
    private final PlayerNameProtector protector;
    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Inject
    public NameProtectorProcessor(PlayerNameProtector protector) {
        this.protector = protector;
    }

    @Override
    public String process(UUID id, String text) {
        ProtectionResult result = protector.maskNames(text);
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
            return protector.restoreNames(text, replacements);
        }
        return text;
    }
}