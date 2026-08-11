package org.omni.placeholder.processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class NormalizerProcessor implements TextProcessor {

    private static final Pattern CODE_PATTERN = Pattern.compile("(?:\\{C\\d+\\})+");
    private static final Pattern SINGLE_PATTERN = Pattern.compile("\\{C\\d+\\}");

    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public String process(UUID id, String text) {
        if (text == null || !text.contains("{C")) return text;

        Matcher matcher = CODE_PATTERN.matcher(text);
        if (!matcher.find()) return text;

        Map<String, String> translationMap = new HashMap<>();
        StringBuilder sb = new StringBuilder(text.length());
        int counter = 0;
        matcher.reset();

        while (matcher.find()) {
            String originalGroup = matcher.group();
            String newPlaceholder = "{C" + counter++ + "}";

            translationMap.put(newPlaceholder, originalGroup);
            matcher.appendReplacement(sb, newPlaceholder);
        }
        matcher.appendTail(sb);

        cache.put(id, translationMap);
        return sb.toString();
    }

    @Override
    public String restore(UUID id, String text) {
        Map<String, String> mappings = cache.getIfPresent(id);
        if (text == null || mappings == null || mappings.isEmpty()) return text;

        cache.invalidate(id);
        Matcher matcher = SINGLE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 50);

        while (matcher.find()) {
            String placeholder = matcher.group();
            String originalSequence = mappings.get(placeholder);

            if (originalSequence != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(originalSequence));
            } else {
                matcher.appendReplacement(sb, placeholder);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}