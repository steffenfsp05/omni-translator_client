package org.omni.placeholder.gradient;

import java.util.Map;

public record ExtractionResult(String cleanText, Map<String, GradientData> gradients) {
}