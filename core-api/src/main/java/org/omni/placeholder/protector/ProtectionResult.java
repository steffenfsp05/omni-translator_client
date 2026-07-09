package org.omni.placeholder.protector;

import java.util.Map;

public record ProtectionResult(String maskedText, Map<String, String> replacements) {
}
