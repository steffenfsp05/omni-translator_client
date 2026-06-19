package org.pytenix.placeholder.protect.impl;

import org.pytenix.placeholder.protect.PlayerNameProtector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultPlayerNameProtector implements PlayerNameProtector {


    private static final Pattern ULTIMATE_PATTERN = Pattern.compile("((?:[&§][0-9a-fk-or])*)([a-zA-Z0-9_]+)");
    private final Set<String> onlinePlayerNames = ConcurrentHashMap.newKeySet();


    public void addPlayer(String name) {
        if (name != null) {
            onlinePlayerNames.add(name.toLowerCase());
        }
    }

    public void removePlayer(String name) {
        if (name != null) {
            onlinePlayerNames.remove(name.toLowerCase());
        }
    }


    public ProtectionResult maskNames(String text) {
        if (text == null || text.isBlank() || onlinePlayerNames.isEmpty()) {
            return new ProtectionResult(text, Collections.emptyMap());
        }

        StringBuilder sb = new StringBuilder();

        Matcher matcher = ULTIMATE_PATTERN.matcher(text);

        Map<String, String> replacements = new HashMap<>();
        int counter = 0;

        while (matcher.find()) {
            String colorPrefix = matcher.group(1);
            String potentialName = matcher.group(2);


            if (onlinePlayerNames.contains(potentialName.toLowerCase())) {

                String placeholder = "{P" + counter + "}";
                replacements.put(placeholder, potentialName);


                String replacement = (colorPrefix != null ? colorPrefix : "") + placeholder;

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                counter++;
            } else {

                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);

        return new ProtectionResult(sb.toString(), replacements);
    }


    public String restoreNames(String translatedText, Map<String, String> replacements) {
        if (replacements.isEmpty()) return translatedText;

        String result = translatedText;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

}

