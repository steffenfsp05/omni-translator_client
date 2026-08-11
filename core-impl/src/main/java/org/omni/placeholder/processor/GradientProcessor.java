package org.omni.placeholder.processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class GradientProcessor implements TextProcessor {

    private static final String COLOR_CODE = "(?:§x(?:§[0-9a-fA-F]){6}|[§&]#[0-9a-fA-F]{6})";
    private static final String FORMAT_CODE = "(?:[§&][l-oK-OrR])";
    private static final String TEXT_CHARS = "[^§&]*";

    private static final Pattern GRADIENT_WORD_PATTERN = Pattern.compile("((?:" + COLOR_CODE + "(?:" + FORMAT_CODE + ")*" + TEXT_CHARS + "){2,})");
    private static final Pattern GRADIENT_HEX_PATTERN = Pattern.compile(COLOR_CODE);
    private static final Pattern FORMAT_PATTERN = Pattern.compile("[§&][l-oK-OrR]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_CLEANER = Pattern.compile("[^0-9a-fA-F]");

    private final Cache<UUID, Map<String, GradientData>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();
    private final Cache<String, Pattern> patternCache = CacheBuilder.newBuilder()
            .maximumSize(100).build();

    record GradientData(Color startColor, Color endColor, boolean bold, boolean italic) {}

    @Override
    public String process(UUID id, String input) {
        if (input == null || input.isBlank()) return input;

        Map<String, GradientData> foundGradients = new HashMap<>();
        String trimmedInput = input.trim();

        Matcher fullMatcher = GRADIENT_WORD_PATTERN.matcher(trimmedInput);
        if (fullMatcher.matches()) {
            foundGradients.put("FULL_LINE", extractColorsAndFormat(fullMatcher.group(1)));
            String cleanText = GRADIENT_HEX_PATTERN.matcher(input).replaceAll("");
            cleanText = FORMAT_PATTERN.matcher(cleanText).replaceAll("");
            cache.put(id, foundGradients);
            return cleanText;
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

        if (!foundGradients.isEmpty()) cache.put(id, foundGradients);
        return sb.toString();
    }

    @Override
    public String restore(UUID id, String text) {
        Map<String, GradientData> gradients = cache.getIfPresent(id);
        if (gradients == null || gradients.isEmpty() || text == null) return text;

        cache.invalidate(id);

        if (gradients.containsKey("FULL_LINE")) {
            return applyGradientToWord(text, gradients.get("FULL_LINE"));
        }

        String result = text;
        for (Map.Entry<String, GradientData> entry : gradients.entrySet()) {
            String tagId = entry.getKey();
            Pattern tagPattern = patternCache.getIfPresent(tagId);
            if (tagPattern == null) {
                tagPattern = Pattern.compile("<" + Pattern.quote(tagId) + ">(.*?)</" + Pattern.quote(tagId) + ">", Pattern.DOTALL);
                patternCache.put(tagId, tagPattern);
            }

            Matcher m = tagPattern.matcher(result);
            if (!m.find()) continue;

            StringBuilder sb = new StringBuilder();
            do {
                String gradientApplied = applyGradientToWord(m.group(1), entry.getValue());
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
        int rStart = info.startColor().getRed(), gStart = info.startColor().getGreen(), bStart = info.startColor().getBlue();
        int rDiff = info.endColor().getRed() - rStart, gDiff = info.endColor().getGreen() - gStart, bDiff = info.endColor().getBlue() - bStart;
        int visibleLength = text.length();

        for (int i = 0; i < visibleLength; i++) {
            float t = (visibleLength > 1) ? (float) i / (visibleLength - 1) : 0;
            sb.append("§#").append(String.format("%02x%02x%02x", (int)(rStart + t * rDiff), (int)(gStart + t * gDiff), (int)(bStart + t * bDiff)));
            if (info.bold()) sb.append("§l");
            if (info.italic()) sb.append("§o");
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private GradientData extractColorsAndFormat(String fullString) {
        Matcher colorMatcher = GRADIENT_HEX_PATTERN.matcher(fullString);
        Color firstColor = null, lastColor = null;
        while (colorMatcher.find()) {
            Color c = parseColor(colorMatcher.group());
            if (firstColor == null) firstColor = c;
            lastColor = c;
        }
        return new GradientData(firstColor, lastColor,
                fullString.contains("§l") || fullString.contains("&l"),
                fullString.contains("§o") || fullString.contains("&o"));
    }

    private Color parseColor(String hexString) {
        return new Color(Integer.parseInt(HEX_CLEANER.matcher(hexString).replaceAll(""), 16));
    }
}