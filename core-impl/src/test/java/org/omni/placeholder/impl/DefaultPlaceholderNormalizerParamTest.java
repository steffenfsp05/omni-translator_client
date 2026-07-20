package org.omni.placeholder.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultPlaceholderNormalizerParamTest {

    private final DefaultPlaceholderNormalizer normalizer = new DefaultPlaceholderNormalizer();
    private final UUID testUuid = UUID.randomUUID();

    @ParameterizedTest(name = "Input {0} sollte zu {1} normalisiert werden")
    @CsvSource({
            "Hallo {C5}, Hallo {C0}",
            "{C10} und {C20}, {C0} und {C1}",
            "Keine Platzhalter, Keine Platzhalter",
            "{C1}{C2}{C3}, {C0}",
            "Mischung {C50} Test, Mischung {C0} Test"
    })
    void testNormalize_VariousInputs(String input, String expected) {
        String result = normalizer.normalizeText(testUuid, input);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "Normalisierung von {0} ergibt {1}")
    @CsvSource({
            "Test {C10} Beispiel, Test {C0} Beispiel",          // Einzelner Platzhalter
            "{C1}{C2} hintereinander, {C0} hintereinander", // Platzhalter-Paare
            "Keine Platzhalter, Keine Platzhalter",             // Negativ-Test
            "Mischmasch {C5} {C10} {C5}, Mischmasch {C0} {C1} {C2}" // Wiederholte Platzhalter
    })
    void testNormalize_MultiplePatterns(String input, String expected) {
        String result = normalizer.normalizeText(testUuid, input);
        assertEquals(expected, result);
    }
}