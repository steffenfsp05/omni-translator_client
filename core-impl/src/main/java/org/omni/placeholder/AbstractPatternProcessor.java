package org.omni.placeholder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPatternProcessor implements TextProcessor {

    private final Pattern pattern;
    private final String prefix;
    private final PlaceholderFormat format;
    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    protected AbstractPatternProcessor(Pattern pattern, String prefix, PlaceholderFormat format) {
        this.pattern = pattern;
        this.prefix = prefix;
        this.format = format;
    }

    @Override
    public String process(UUID id, String text) {
        if (text == null || text.isBlank()) return text;

        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return text;

        matcher.reset();
        Map<String, String> replacements = new HashMap<>();
        StringBuilder sb = new StringBuilder(text.length());
        int counter = 0;

        while (matcher.find()) {
            String original = matcher.group();
            String placeholder = format.format(prefix, counter++);

            replacements.put(placeholder, original);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);

        cache.put(id, replacements);
        return sb.toString();
    }

    @Override
    public String restore(UUID id, String text) {
        if (text == null || text.isBlank()) return text;

        Map<String, String> replacements = cache.getIfPresent(id);
        if (replacements == null || replacements.isEmpty()) return text;

        cache.invalidate(id);
        String restored = text;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }
}