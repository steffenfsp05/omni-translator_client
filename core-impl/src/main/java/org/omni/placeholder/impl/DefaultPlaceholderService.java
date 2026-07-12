package org.omni.placeholder.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.omni.placeholder.BasePlaceholder;
import org.omni.placeholder.normalizer.PlaceholderNormalizer;
import org.omni.placeholder.protector.PlayerNameProtector;
import org.omni.placeholder.protector.ProtectionResult;
import org.omni.placeholder.protector.WordProtector;
import org.omni.placeholder.service.PlaceholderService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Singleton
public class DefaultPlaceholderService implements PlaceholderService {

    //NOTE: KI GENERATED/IMPROVED
    public static final Pattern PRICE_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)*");
    public static final Pattern SYSTEM_PROTECTION_PATTERN = Pattern.compile("(?:\\{[a-zA-Z]\\d+\\})|(?:\\[#[A-Z]+-\\d+#\\])|(?:</?[HAG]\\d+>)");
    public static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§x(?:§[0-9a-f]){6}|§#[0-9a-f]{6}|§[0-9a-fk-or]");


    final PlaceholderNormalizer placeholderNormalizer;
    final PlayerNameProtector playerNameProtector;
    final WordProtector wordProtector;


    public TreeMap<Integer, BasePlaceholder> registeredPlaceholders;


    Cache<UUID, ProtectionResult> cachedNames = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS).build();

    Cache<UUID, ProtectionResult> cachedWords = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS).build();


    private Pattern atomicPattern;
    private List<BasePlaceholder> indexedPlaceholders;


    @Inject
    public DefaultPlaceholderService(
            PlaceholderNormalizer placeholderNormalizer,
            PlayerNameProtector playerNameProtector,
            WordProtector wordProtector
    ) {

        this.placeholderNormalizer = placeholderNormalizer;
        this.playerNameProtector = playerNameProtector;
        this.wordProtector = wordProtector;

        this.registeredPlaceholders = new TreeMap<>();


        // if (translatorService.getTranslationConfiguration() != null) {
        //     ServerConfiguration serverConfiguration = translatorService.getTranslationConfiguration();
        //     updateProtectedWords(serverConfiguration != null ? serverConfiguration.getBlacklistedWords() : new HashSet<>());

        //  }

        registerPlaceholder(0, new ExtendedPlaceholder("SKIP", () -> SYSTEM_PROTECTION_PATTERN));
        registerPlaceholder(1, new ExtendedPlaceholder("C", () -> COLOR_PATTERN));
        registerPlaceholder(10, new ExtendedPlaceholder("N", () -> PRICE_PATTERN));
    }


    public void updateProtectedWords(Set<String> words) {
        this.wordProtector.build(words);
    }

    public boolean registerPlaceholder(int priority, BasePlaceholder placeholder) {
        if (registeredPlaceholders.containsKey(priority)) {
            System.out.println("Could not register Placeholder: " + placeholder.getClass().getSimpleName() + " Priority already registered!");
            return false;
        }
        registeredPlaceholders.put(priority, placeholder);

        rebuildAtomicPattern();
        return true;
    }


    public void rebuildAtomicPattern() {
        this.indexedPlaceholders = new ArrayList<>(registeredPlaceholders.values());

        String combinedRegex = indexedPlaceholders.stream()
                .filter(ph -> ph.getPattern() != null)

                .map(ph -> "(" + ph.getPattern().pattern() + ")")
                .collect(Collectors.joining("|"));

        if (!combinedRegex.isEmpty()) {
            this.atomicPattern = Pattern.compile(combinedRegex);
        } else {
            this.atomicPattern = null;
        }
    }


    public String toPlaceholders(UUID id, String text) {
        if (atomicPattern == null || text == null || text.isEmpty()) return text;


        ProtectionResult result = getPlayerNameProtector().maskNames(text);
        if (!result.replacements().isEmpty()) {
            cachedNames.put(id, result);
            text = result.maskedText();
        }

        StringBuilder sb = new StringBuilder();
        Matcher matcher = atomicPattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());

            boolean matched = false;

            for (int i = 0; i < indexedPlaceholders.size(); i++) {

                if (i + 1 <= matcher.groupCount() && matcher.group(i + 1) != null) {
                    BasePlaceholder ph = indexedPlaceholders.get(i);
                    String originalValue = matcher.group(i + 1);

                    if (ph.placeholder().equals("SKIP")) {
                        sb.append(originalValue);
                        matched = true;
                        break;
                    }
                    // -----------------------------

                    Map<Integer, String> playerCache = ph.cachedValues().asMap()
                            .computeIfAbsent(id, k -> new HashMap<>());

                    int contentId = playerCache.size();
                    playerCache.put(contentId, originalValue);

                    sb.append("{").append(ph.placeholder()).append(contentId).append("}");
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                sb.append(matcher.group());
            }
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));


        text = sb.toString();

        ProtectionResult wordResult = wordProtector.protect(text);
        if (!wordResult.replacements().isEmpty()) {
            cachedWords.put(id, wordResult);
            text = wordResult.maskedText();
        }


        return placeholderNormalizer.normalizeText(id, text);
    }

    public String fromPlaceholders(UUID id, String text) {
        List<BasePlaceholder> reverseList = new ArrayList<>(registeredPlaceholders.values());
        Collections.reverse(reverseList);

        text = placeholderNormalizer.denormalizeText(id, text);
        for (BasePlaceholder ph : reverseList) {
            if (ph.placeholder().equals("SKIP")) continue;

            Map<Integer, String> cache = ph.cachedValues().getIfPresent(id);
            if (cache == null || cache.isEmpty()) continue;

            for (Map.Entry<Integer, String> entry : cache.entrySet()) {

                String token = "{" + ph.placeholder() + entry.getKey() + "}";
                text = text.replace(token, entry.getValue());
            }
        }


        if (cachedNames.getIfPresent(id) != null) {
            text = getPlayerNameProtector().restoreNames(text, cachedNames.getIfPresent(id).replacements());
            cachedNames.invalidate(id);
        }

        if (cachedWords.getIfPresent(id) != null) {
            text = wordProtector.restore(text, cachedWords.getIfPresent(id).replacements());
            cachedWords.invalidate(id);
        }
        return text;
    }
}
