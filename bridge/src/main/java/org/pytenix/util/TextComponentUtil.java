package org.pytenix.util;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.pytenix.translation.TranslatorService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextComponentUtil {

    private static final Pattern SANITIZE_PATTERN = Pattern.compile("§(?![0-9a-fA-Fk-oK-OrRxX#])");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?s)<([AH])(\\d+)>((?:(?!<[AH]\\d+>).)*?)</\\1\\2>");

    private final TranslatorService translatorService;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final AsyncCache<TranslationKey, Component> translationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .buildAsync();

    public TextComponentUtil(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }

    public CompletableFuture<Component> translateComplexMessage(Component originalComponent, String lang, String module) {
        TranslationKey key = new TranslationKey(originalComponent, lang, module);
        return translationCache.get(key, (k, executor) -> doTranslateComplexMessage(k.component(), k.lang(), k.module()));
    }

    public String sanitizeLegacyText(String text) {
        if (text == null || text.isEmpty()) return "";
        String sanitized = SANITIZE_PATTERN.matcher(text).replaceAll("");
        if (sanitized.endsWith("§")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private CompletableFuture<Component> doTranslateComplexMessage(Component originalComponent, String lang, String module) {
        TranslationContext ctx = new TranslationContext();
        Component taggedComponent = injectTags(originalComponent, ctx);
        String mainPayload = legacySerializer.serialize(taggedComponent);

        Map<Integer, CompletableFuture<String>> hoverFutures = new HashMap<>();

        for (Map.Entry<Integer, Component> entry : ctx.hovers.entrySet()) {
            int id = entry.getKey();
            String legacyHover = legacySerializer.serialize(entry.getValue());

            hoverFutures.put(id, translatorService.translate(legacyHover, lang, module).exceptionally(ex -> {
                System.err.println("Translation failed for Hover ID " + id + ": " + ex.getMessage());
                return legacyHover;
            }));
        }

        CompletableFuture<String> mainFuture = translatorService.translate(mainPayload, lang, module).exceptionally(ex -> {
            System.err.println("Translation failed for main payload: " + ex.getMessage());
            return mainPayload;
        });

        return mainFuture.thenCombineAsync(
                CompletableFuture.allOf(hoverFutures.values().toArray(new CompletableFuture[0])),
                (translatedMainText, v) -> processTranslations(translatedMainText, hoverFutures, ctx)
        );
    }

    private Component processTranslations(String translatedMainText, Map<Integer, CompletableFuture<String>> hoverFutures, TranslationContext ctx) {
        String cleanMainText = sanitizeLegacyText(translatedMainText);
        List<Map.Entry<String, Component>> replacements = new ArrayList<>();
        boolean found;

        do {
            found = false;
            Matcher m = TAG_PATTERN.matcher(cleanMainText);
            StringBuilder sb = new StringBuilder();

            while (m.find()) {
                found = true;
                String type = m.group(1);
                int id = Integer.parseInt(m.group(2));
                String inner = m.group(3);

                String replacementKey = UUID.randomUUID().toString().replace("-", "");

                Component innerComp = legacySerializer.deserialize(inner);

                if ("A".equals(type)) {
                    ClickEvent originalClick = ctx.clicks.get(id);
                    if (originalClick != null) {
                        innerComp = innerComp.clickEvent(originalClick);
                    }
                } else if ("H".equals(type)) {
                    String translatedHoverText = sanitizeLegacyText(hoverFutures.get(id).join());
                    Component hoverComp = legacySerializer.deserialize(translatedHoverText);
                    innerComp = innerComp.hoverEvent(HoverEvent.showText(hoverComp));
                }

                replacements.add(new AbstractMap.SimpleEntry<>(replacementKey, innerComp));
                m.appendReplacement(sb, replacementKey);
            }
            m.appendTail(sb);
            cleanMainText = sb.toString();
        } while (found);

        Component finalComponent = legacySerializer.deserialize(cleanMainText);

        for (int i = replacements.size() - 1; i >= 0; i--) {
            Map.Entry<String, Component> entry = replacements.get(i);
            finalComponent = finalComponent.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
                    .build());
        }

        return finalComponent;
    }

    private Component injectTags(Component c, TranslationContext ctx) {
        List<Component> newChildren = new ArrayList<>(c.children().size());
        for (Component child : c.children()) {
            newChildren.add(injectTags(child, ctx));
        }

        Component modified = c.children(newChildren);

        ClickEvent click = modified.clickEvent();
        HoverEvent<?> hover = modified.hoverEvent();

        if (click != null || hover != null) {
            StringBuilder startTag = new StringBuilder();
            StringBuilder endTag = new StringBuilder();

            if (click != null) {
                int id = ctx.clickIndex++;
                ctx.clicks.put(id, click);
                startTag.append("<A").append(id).append(">");
                endTag.insert(0, "</A" + id + ">");
            }
            if (hover != null) {
                int id = ctx.hoverIndex++;
                ctx.hovers.put(id, (Component) hover.value());
                startTag.append("<H").append(id).append(">");
                endTag.insert(0, "</H" + id + ">");
            }

            Component nodeWithoutEvents = modified.clickEvent(null).hoverEvent(null);

            return Component.empty()
                    .append(Component.text(startTag.toString()))
                    .append(nodeWithoutEvents)
                    .append(Component.text(endTag.toString()));
        }

        return modified;
    }

    private record TranslationKey(Component component, String lang, String module) {
    }

    private static class TranslationContext {
        final Map<Integer, ClickEvent> clicks = new HashMap<>();
        final Map<Integer, Component> hovers = new HashMap<>();
        int clickIndex = 0;
        int hoverIndex = 0;
    }
}