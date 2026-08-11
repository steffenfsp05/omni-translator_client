package org.omni.placeholder.processor;

import com.google.inject.Singleton;
import org.omni.placeholder.AbstractPatternProcessor;
import org.omni.placeholder.PlaceholderFormat;

import java.util.regex.Pattern;

@Singleton
public class PriceProcessor extends AbstractPatternProcessor {
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?<!\\{[a-zA-Z]{1,10}|</?[a-zA-Z]{1,10}|[a-zA-Z]{1,10}-)(?<!\\d)\\d+(?:[.,]\\d+)*(?!\\d)"
    );
    public PriceProcessor() {
        super(PRICE_PATTERN, "N", PlaceholderFormat.BRACKET);
    }
}