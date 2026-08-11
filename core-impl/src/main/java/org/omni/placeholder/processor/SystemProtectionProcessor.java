package org.omni.placeholder.processor;

import com.google.inject.Singleton;
import org.omni.placeholder.AbstractPatternProcessor;
import org.omni.placeholder.PlaceholderFormat;

import java.util.regex.Pattern;

@Singleton
public class SystemProtectionProcessor extends AbstractPatternProcessor {
    private static final Pattern SYSTEM_PATTERN = Pattern.compile("(?:\\{[a-zA-Z]+\\d+\\})|(?:\\[#[A-Z]+-\\d+#\\])|(?:</?[a-zA-Z]+\\d+>)");

    public SystemProtectionProcessor() {
        super(SYSTEM_PATTERN, "SKIP", PlaceholderFormat.BRACKET);
    }
}