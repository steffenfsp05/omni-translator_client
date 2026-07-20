package org.omni.placeholder.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DefaultPlaceholderNormalizerTest {

    private DefaultPlaceholderNormalizer normalizer;
    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        normalizer = new DefaultPlaceholderNormalizer();
    }

    @Test
    void testNormalize_NullOrEmpty() {
        assertEquals(null, normalizer.normalizeText(testUuid, null));
        assertEquals("Ohne Codes", normalizer.normalizeText(testUuid, "Ohne Codes"));
    }

    @Test
    void testNormalize_StandardReplacement() {
        String input = "Das ist {C15} und {C99}";
        String result = normalizer.normalizeText(testUuid, input);

        assertEquals("Das ist {C0} und {C1}", result);
    }

    @Test
    void testDenormalize_CacheHit() {
        String input = "Farbe {C12} und {C44}";

        String normalized = normalizer.normalizeText(testUuid, input);
        assertEquals("Farbe {C0} und {C1}", normalized);

        String translated = "Color {C0} and {C1}";

        String denormalized = normalizer.denormalizeText(testUuid, translated);
        assertEquals("Color {C12} and {C44}", denormalized);
    }

    @Test
    void testDenormalize_CacheMiss() {
        String input = "Color {C0}";
        String result = normalizer.denormalizeText(testUuid, input);

        assertEquals(input, result);
    }
    @Test
    void testNormalize_MultiUserIsolation() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String inputA = "{C15}";
        String inputB = "{C99}";

        String normA = normalizer.normalizeText(userA, inputA);
        String normB = normalizer.normalizeText(userB, inputB);

        assertEquals("{C0}", normA);
        assertEquals("{C0}", normB);

        assertEquals("{C15}", normalizer.denormalizeText(userA, normA));
        assertEquals("{C99}", normalizer.denormalizeText(userB, normB));
    }

    @Test
    void testNormalize_RepeatedPlaceholders() {
        String input = "Text {C5} wiederholung {C5} ende";

        String normalized = normalizer.normalizeText(testUuid, input);

        assertEquals("Text {C0} wiederholung {C1} ende", normalized);

        String denormalized = normalizer.denormalizeText(testUuid, normalized);
        assertEquals(input, denormalized);
    }

    @Test
    void testNormalize_ComplexMixedString() {
        String input = "§aPlayer {C10} &bmit {C20} und {C10}";

        String normalized = normalizer.normalizeText(testUuid, input);

        String expected = "§aPlayer {C0} &bmit {C1} und {C2}";
        assertEquals(expected, normalized);

        assertEquals(input, normalizer.denormalizeText(testUuid, normalized));
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testNormalize_AdjacentPlaceholders_AreMergedIntoOneToken() {
        String input = "abc{C1}{C2}def";

        String normalized = normalizer.normalizeText(testUuid, input);
        assertEquals("abc{C0}def", normalized);

        String denormalized = normalizer.denormalizeText(testUuid, normalized);
        assertEquals(input, denormalized);
    }

    @Test
    void testNormalize_MalformedCodeWithoutDigits_IsIgnored() {
        String input = "Text {C} ohne Ziffer";

        String result = normalizer.normalizeText(testUuid, input);

        assertEquals(input, result);
    }

    @Test
    void testDenormalize_PreviousNormalizationHadNoPlaceholders_TextPassesThroughUnchanged() {
        normalizer.normalizeText(testUuid, "Text ganz ohne Platzhalter");

        String result = normalizer.denormalizeText(testUuid, "Beliebiger Text {C0}");

        assertEquals("Beliebiger Text {C0}", result);
    }

    @Test
    void testDenormalize_UnknownToken_RemainsUnchanged() {
        normalizer.normalizeText(testUuid, "{C7}");

        String result = normalizer.denormalizeText(testUuid, "{C0} und {C99}");

        assertEquals("{C7} und {C99}", result);
    }

    @Test
    void testNormalize_SecondCallForSameUuid_OverwritesPreviousMapping() {
        normalizer.normalizeText(testUuid, "{C1}");
        normalizer.normalizeText(testUuid, "{C99}");

        assertEquals("{C99}", normalizer.denormalizeText(testUuid, "{C0}"));
    }
}