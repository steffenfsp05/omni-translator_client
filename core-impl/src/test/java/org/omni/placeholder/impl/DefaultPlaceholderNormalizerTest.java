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

        assertEquals("", result);
    }
    @Test
    void testNormalize_MultiUserIsolation() {
        // Sicherstellen, dass User A und User B sich nicht gegenseitig beeinflussen
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String inputA = "{C15}"; // User A hat ein spezielles Mapping
        String inputB = "{C99}"; // User B hat ein anderes

        String normA = normalizer.normalizeText(userA, inputA);
        String normB = normalizer.normalizeText(userB, inputB);

        // Beide starten bei Index 0 für den ersten Platzhalter des Users
        assertEquals("{C0}", normA);
        assertEquals("{C0}", normB);

        // Denormalisierung darf nur die eigenen Werte zurückgeben
        assertEquals("{C15}", normalizer.denormalizeText(userA, normA));
        assertEquals("{C99}", normalizer.denormalizeText(userB, normB));
    }

    @Test
    void testNormalize_RepeatedPlaceholders() {
        // Testet, ob das System erkennt, wenn derselbe Platzhalter mehrfach vorkommt
        String input = "Text {C5} wiederholung {C5} ende";

        String normalized = normalizer.normalizeText(testUuid, input);

        // Erwartung: Beide {C5} sollten zum selben Index {C0} gemappt werden
        assertEquals("Text {C0} wiederholung {C1} ende", normalized);

        // Denormalisierung muss beide wieder korrekt auflösen
        String denormalized = normalizer.denormalizeText(testUuid, normalized);
        assertEquals(input, denormalized);
    }

    @Test
    void testNormalize_ComplexMixedString() {
        // Testet eine Mischung aus Farben, Text und mehreren verschiedenen Platzhaltern
        String input = "§aPlayer {C10} &bmit {C20} und {C10}";

        String normalized = normalizer.normalizeText(testUuid, input);

        // {C10} -> {C0}, {C20} -> {C1}
        String expected = "§aPlayer {C0} &bmit {C1} und {C2}";
        assertEquals(expected, normalized);

        assertEquals(input, normalizer.denormalizeText(testUuid, normalized));
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testNormalize_AdjacentPlaceholders_AreMergedIntoOneToken() {
        // Direkt aneinandergereihte {C..}-Codes werden vom Pattern als EIN Block erkannt
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

        // Da beim letzten normalize() keine Mappings erzeugt wurden, bleibt der Text unangetastet
        assertEquals("Beliebiger Text {C0}", result);
    }

    @Test
    void testDenormalize_UnknownToken_RemainsUnchanged() {
        normalizer.normalizeText(testUuid, "{C7}");

        String result = normalizer.denormalizeText(testUuid, "{C0} und {C99}");

        // {C99} war nie Teil der ursprünglichen Normalisierung -> bleibt unverändert
        assertEquals("{C7} und {C99}", result);
    }

    @Test
    void testNormalize_SecondCallForSameUuid_OverwritesPreviousMapping() {
        normalizer.normalizeText(testUuid, "{C1}");
        normalizer.normalizeText(testUuid, "{C99}");

        // Nur das zuletzt erzeugte Mapping ist noch gültig
        assertEquals("{C99}", normalizer.denormalizeText(testUuid, "{C0}"));
    }
}