package org.pytenix.util;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.pytenix.TranslatorService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextComponentUtil {
    private final TranslatorService translatorService;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern SANITIZE_PATTERN = Pattern.compile("§(?![0-9a-fA-Fk-oK-OrRxX#])");
    private static final Pattern PATTERN = Pattern.compile("(?s)<([AH])(\\d+)>((?:(?!<[AH]\\d+>).)*?)</\\1\\2>");

    private final AsyncCache<TranslationKey, Component> translationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .buildAsync();

    private record TranslationKey(Component component, String lang, String module) {}

    public TextComponentUtil(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }

    private static class TranslationContext {
        int clickIndex = 0;
        int hoverIndex = 0;
        final Map<Integer, ClickEvent> clicks = new HashMap<>();
        final Map<Integer, Component> hovers = new HashMap<>();
    }

    public CompletableFuture<Component> translateComplexMessage(Component originalComponent, String lang, String module) {
        TranslationKey key = new TranslationKey(originalComponent, lang, module);
        return translationCache.get(key, (k, executor) -> doTranslateComplexMessage(k.component(), k.lang(), k.module()));
    }

    public String sanitizeLegacyText(String text) {
        if (text == null) return "";
        String sanitized = SANITIZE_PATTERN.matcher(text).replaceAll("");
        if (sanitized.endsWith("§")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private CompletableFuture<Component> doTranslateComplexMessage(Component originalComponent, String lang, String module) {
        long started = System.currentTimeMillis();
        TranslationContext ctx = new TranslationContext();

        Component taggedComponent = injectTags(originalComponent, ctx);
        String mainPayload = legacySerializer.serialize(taggedComponent);

        UUID mainBatchId = UUID.randomUUID();
        Map<Integer, CompletableFuture<String>> hoverFutures = new HashMap<>();

        for (Map.Entry<Integer, Component> entry : ctx.hovers.entrySet()) {
            int id = entry.getKey();
            UUID hoverBatchId = UUID.randomUUID();
            String legacyHover = legacySerializer.serialize(entry.getValue());
            String preparedHover = translatorService.preparePayload(hoverBatchId, legacyHover);
            hoverFutures.put(id, translatorService.processAndRestore(hoverBatchId, preparedHover, lang, module, started));
        }

        String preparedMain = translatorService.preparePayload(mainBatchId, mainPayload);
        CompletableFuture<String> mainFuture = translatorService.processAndRestore(mainBatchId, preparedMain, lang, module, started);
        return mainFuture.thenCombineAsync(
                CompletableFuture.allOf(hoverFutures.values().toArray(new CompletableFuture[0])),
                (translatedMainText, v) -> {
                    String cleanMainText = sanitizeLegacyText(translatedMainText);

                    List<Map.Entry<String, Component>> replacements = new ArrayList<>();
                    boolean found;

                    // Iteratively extract the INNERMOST tags first, handling infinite nesting!
                    do {
                        found = false;
                        // Matches <A> or <H> that do NOT contain another <A> or <H> inside them
                        Matcher m = PATTERN.matcher(cleanMainText);
                        StringBuffer sb = new StringBuffer();

                        while (m.find()) {
                            found = true;
                            String type = m.group(1);
                            int id = Integer.parseInt(m.group(2));
                            String inner = m.group(3);
                            String uuid = UUID.randomUUID().toString();

                            Component innerComp = legacySerializer.deserialize(inner);

                            if (type.equals("A")) {
                                ClickEvent originalClick = ctx.clicks.get(id);
                                innerComp = innerComp.clickEvent(originalClick);
                            } else if (type.equals("H")) {
                                String translatedHoverText = sanitizeLegacyText(hoverFutures.get(id).join());
                                Component hoverComp = legacySerializer.deserialize(translatedHoverText);
                                innerComp = innerComp.hoverEvent(HoverEvent.showText(hoverComp));
                            }

                            replacements.add(new AbstractMap.SimpleEntry<>(uuid, innerComp));
                            m.appendReplacement(sb, uuid);
                        }
                        m.appendTail(sb);
                        cleanMainText = sb.toString();
                    } while (found);

                    Component finalComponent = legacySerializer.deserialize(cleanMainText);

                    Collections.reverse(replacements);
                    for (Map.Entry<String, Component> entry : replacements) {
                        finalComponent = finalComponent.replaceText(TextReplacementConfig.builder()
                                .matchLiteral(entry.getKey())
                                .replacement(entry.getValue())
                                .build());
                    }

                    return finalComponent;
                });
    }

    private Component injectTags(Component c, TranslationContext ctx) {
        List<Component> newChildren = new ArrayList<>();
        for (Component child : c.children()) {
            newChildren.add(injectTags(child, ctx));
        }

        Component modified = c.children(newChildren);

        ClickEvent click = modified.clickEvent();
        HoverEvent<?> hover = modified.hoverEvent();

        if (click != null || hover != null) {
            String startTag = "";
            String endTag = "";

            if (click != null) {
                int id = ctx.clickIndex++;
                ctx.clicks.put(id, click);
                startTag += "<A" + id + ">";
                endTag = "</A" + id + ">" + endTag;
            }
            if (hover != null) {
                int id = ctx.hoverIndex++;
                ctx.hovers.put(id, (Component) hover.value());
                startTag += "<H" + id + ">";
                endTag = "</H" + id + ">" + endTag;
            }

            Component nodeWithoutEvents = modified.clickEvent(null).hoverEvent(null);

            return Component.empty()
                    .append(Component.text(startTag))
                    .append(nodeWithoutEvents)
                    .append(Component.text(endTag));
        }

        return modified;
    }
}