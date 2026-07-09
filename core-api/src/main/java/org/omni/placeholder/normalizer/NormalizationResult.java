package org.omni.placeholder.normalizer;

import java.util.Map;

public record NormalizationResult(String cleanedText, Map<String, String> mappings) {
}
