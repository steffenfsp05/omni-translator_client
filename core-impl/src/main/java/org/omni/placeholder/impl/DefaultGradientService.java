package org.omni.placeholder.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Singleton;
import org.jetbrains.annotations.Nullable;
import org.omni.placeholder.gradient.ExtractionResult;
import org.omni.placeholder.gradient.GradientData;
import org.omni.placeholder.gradient.GradientService;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class DefaultGradientService implements GradientService {

    private static final String COLOR_CODE = "(?:§x(?:§[0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6})";
    private static final String FORMAT_CODE = "(?:[§&][l-oK-OrR])";
    private static final String TEXT_CHARS = "[^§&]+";

    private static final Pattern GRADIENT_WORD_PATTERN = Pattern.compile("((?:" + COLOR_CODE + "(?:" + FORMAT_CODE + ")*" + TEXT_CHARS + "){2,})");
    private static final Pattern GRADIENT_HEX_PATTERN = Pattern.compile(COLOR_CODE);
    private static final Pattern FORMAT_PATTERN = Pattern.compile("[§&][l-oK-OrR]", Pattern.CASE_INSENSITIVE);


    private static final Pattern HEX_CLEANER = Pattern.compile("[^0-9a-fA-F]");

    private final Cache<UUID, Map<String, GradientData>> cachedGradients = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    private final Cache<String, Pattern> patternCache = Caffeine.newBuilder()
            .maximumSize(100)
            .build();

    @Override
    public ExtractionResult stripAndAnalyze(String input) {
        if (input == null || input.isEmpty()) return new ExtractionResult(input, new HashMap<>());

        Map<String, GradientData> foundGradients = new HashMap<>();
        String trimmedInput = input.trim();

        Matcher fullMatcher = GRADIENT_WORD_PATTERN.matcher(trimmedInput);
        if (fullMatcher.matches()) {
            String fullGradientString = fullMatcher.group(1);
            GradientData data = extractColorsAndFormat(fullGradientString);
            foundGradients.put("FULL_LINE", data);

            String cleanText = GRADIENT_HEX_PATTERN.matcher(input).replaceAll("");
            cleanText = FORMAT_PATTERN.matcher(cleanText).replaceAll("");
            return new ExtractionResult(cleanText, foundGradients);
        }

        Matcher m = GRADIENT_WORD_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        int gradientCounter = 0;

        while (m.find()) {
            String fullGradientString = m.group(1);
            GradientData data = extractColorsAndFormat(fullGradientString);

            String cleanWord = GRADIENT_HEX_PATTERN.matcher(fullGradientString).replaceAll("");
            cleanWord = FORMAT_PATTERN.matcher(cleanWord).replaceAll("");

            String tagId = "G" + gradientCounter;
            String replacement = "<" + tagId + ">" + cleanWord + "</" + tagId + ">";

            foundGradients.put(tagId, data);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            gradientCounter++;
        }
        m.appendTail(sb);

        return new ExtractionResult(sb.toString(), foundGradients);
    }

    @Override
    public String restoreGradients(UUID uuid, String translatedText) {
        Map<String, GradientData> gradients = cachedGradients.getIfPresent(uuid);
        if (gradients == null || gradients.isEmpty()) return translatedText;

        if (gradients.containsKey("FULL_LINE")) {
            return applyGradientToWord(translatedText, gradients.get("FULL_LINE"));
        }

        String result = translatedText;

        for (Map.Entry<String, GradientData> entry : gradients.entrySet()) {
            String tagId = entry.getKey();
            Pattern tagPattern = patternCache.get(tagId, id ->
                    Pattern.compile("<" + Pattern.quote(id) + ">(.*?)</" + Pattern.quote(id) + ">", Pattern.DOTALL)
            );

            Matcher m = tagPattern.matcher(result);
            if (!m.find()) continue;

            StringBuilder sb = new StringBuilder();
            do {
                String translatedWord = m.group(1);
                String gradientApplied = applyGradientToWord(translatedWord, entry.getValue());
                m.appendReplacement(sb, Matcher.quoteReplacement(gradientApplied));
            } while (m.find());

            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    private String applyGradientToWord(String text, GradientData info) {
        if (text == null || text.isEmpty()) return text;

        if (info.startColor().equals(info.endColor())) {
            StringBuilder sb = new StringBuilder(text.length() + 16);
            sb.append("§#").append(String.format("%02x%02x%02x", info.startColor().getRed(), info.startColor().getGreen(), info.startColor().getBlue()));
            if (info.bold()) sb.append("§l");
            if (info.italic()) sb.append("§o");
            sb.append(text);
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder(text.length() * 16);

        int rStart = info.startColor().getRed();
        int gStart = info.startColor().getGreen();
        int bStart = info.startColor().getBlue();

        int rDiff = info.endColor().getRed() - rStart;
        int gDiff = info.endColor().getGreen() - gStart;
        int bDiff = info.endColor().getBlue() - bStart;

        int visibleLength = text.length();

        for (int i = 0; i < text.length(); i++) {
            float t = (visibleLength > 1) ? (float) i / (visibleLength - 1) : 0;

            int r = (int) (rStart + t * rDiff);
            int g = (int) (gStart + t * gDiff);
            int b = (int) (bStart + t * bDiff);

            sb.append("§#").append(String.format("%02x%02x%02x", r, g, b));
            if (info.bold()) sb.append("§l");
            if (info.italic()) sb.append("§o");
            sb.append(text.charAt(i));
        }

        return sb.toString();
    }

    private GradientData extractColorsAndFormat(String fullGradientString) {
        Matcher colorMatcher = GRADIENT_HEX_PATTERN.matcher(fullGradientString);
        Color firstColor = null;
        Color lastColor = null;
        while (colorMatcher.find()) {
            Color c = parseColor(colorMatcher.group());
            if (firstColor == null) firstColor = c;
            lastColor = c;
        }

        boolean isBold = fullGradientString.contains("§l") || fullGradientString.contains("&l");
        boolean isItalic = fullGradientString.contains("§o") || fullGradientString.contains("&o");

        return new GradientData(firstColor, lastColor, isBold, isItalic);
    }


    private Color parseColor(String hexString) {
        String cleanHex = HEX_CLEANER.matcher(hexString).replaceAll("");
        return new Color(Integer.parseInt(cleanHex, 16));
    }

    @Override public void cacheGradient(UUID uuid, Map<String, GradientData> gradients) { cachedGradients.put(uuid, gradients); }
    @Override public void invalidCachedGradient(UUID uuid) { cachedGradients.invalidate(uuid); }
    @Override public @Nullable Map<String, GradientData> getCachedGradient(UUID uuid) { return cachedGradients.getIfPresent(uuid); }
}