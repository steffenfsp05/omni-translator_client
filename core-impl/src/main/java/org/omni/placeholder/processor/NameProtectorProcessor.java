package org.omni.placeholder.processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.event.register.player.OmniPlayerDisconnectEvent;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class NameProtectorProcessor implements TextProcessor {

    private static final Pattern ULTIMATE_PATTERN = Pattern.compile("((?:[&§][0-9a-fk-or])*)([a-zA-Z0-9_]+)");
    private final Set<String> onlinePlayerNames = ConcurrentHashMap.newKeySet();
    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @OmniSubscribe(priority = 91)
    public void onConnect(PlayerConnectEvent event) {
        if (event.playerName() != null) {
            onlinePlayerNames.add(event.playerName().toLowerCase());
        }
    }

    @OmniSubscribe(priority = 91)
    public void onDisconnect(OmniPlayerDisconnectEvent event) {
        if (event.playerName() != null) {
            onlinePlayerNames.remove(event.playerName().toLowerCase());
        }
    }

    @Override
    public String process(UUID id, String text) {
        if (text == null || text.isBlank() || onlinePlayerNames.isEmpty()) return text;

        StringBuilder sb = new StringBuilder();
        Matcher matcher = ULTIMATE_PATTERN.matcher(text);
        Map<String, String> replacements = new HashMap<>();
        int counter = 0;

        while (matcher.find()) {
            String colorPrefix = matcher.group(1);
            String potentialName = matcher.group(2);

            if (onlinePlayerNames.contains(potentialName.toLowerCase())) {
                String placeholder = "<P" + counter + ">";
                replacements.put(placeholder, potentialName);

                String replacement = (colorPrefix != null ? colorPrefix : "") + placeholder;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                counter++;
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);

        if (!replacements.isEmpty()) cache.put(id, replacements);
        return sb.toString();
    }

    @Override
    public String restore(UUID id, String text) {
        Map<String, String> replacements = cache.getIfPresent(id);
        if (replacements == null || replacements.isEmpty() || text == null) return text;

        cache.invalidate(id);
        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}