package org.omni.placeholder.processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Singleton
public class WordProtectorProcessor implements TextProcessor {

    private Trie trie;
    private final Cache<UUID, Map<String, String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @OmniSubscribe(priority = 90)
    public void onUpdate(ConfigUpdateEvent event) {
        if (event.translationConfiguration() != null) {
            Set<String> words = event.translationConfiguration().getBlacklistedWords();
            if (words == null || words.isEmpty()) {
                this.trie = null;
                return;
            }
            Trie.TrieBuilder builder = Trie.builder().onlyWholeWords();
            for (String word : words) {
                if (word.length() > 1) builder.addKeyword(word);
            }
            this.trie = builder.build();
        }
    }

    @Override
    public String process(UUID id, String text) {
        if (trie == null || text == null || text.isBlank()) return text;

        Collection<Emit> emits = trie.parseText(text);
        if (emits.isEmpty()) return text;

        Map<String, String> replacements = new HashMap<>();
        StringBuilder sb = new StringBuilder(text);

        List<Emit> sortedEmits = new ArrayList<>(emits);
        sortedEmits.sort(Comparator.comparingInt(Emit::getStart).reversed());

        int counter = 0;
        for (Emit emit : sortedEmits) {
            String placeholder = "<W" + counter++ + ">";
            replacements.put(placeholder, emit.getKeyword());
            sb.replace(emit.getStart(), emit.getEnd() + 1, placeholder);
        }

        cache.put(id, replacements);
        return sb.toString();
    }

    @Override
    public String restore(UUID id, String text) {
        Map<String, String> replacements = cache.getIfPresent(id);
        if (replacements == null || replacements.isEmpty() || text == null) return text;

        cache.invalidate(id);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}