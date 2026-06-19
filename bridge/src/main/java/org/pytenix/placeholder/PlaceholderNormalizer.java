package org.pytenix.placeholder;

import java.util.Map;
import java.util.UUID;

public interface PlaceholderNormalizer {


    String normalizeText(UUID uuid, String text);

    String denormalizeText(UUID uuid, String text);


    record NormalizationResult(String cleanedText, Map<String, String> mappings) {
    }


}
