package org.omni.placeholder.processor;

import com.google.inject.Singleton;
import org.omni.placeholder.AbstractPatternProcessor;
import org.omni.placeholder.PlaceholderFormat;

import java.util.regex.Pattern;

@Singleton
public class ColorProcessor extends AbstractPatternProcessor {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§x(?:§[0-9a-f]){6}|[§&]#[0-9a-f]{6}|[§&][0-9a-fk-or]");

    public ColorProcessor() {
        super(COLOR_PATTERN, "C", PlaceholderFormat.BRACKET);
    }
}