package org.omni.placeholder.protect.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.omni.placeholder.protector.ProtectionResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWordProtectorTest {

    private DefaultWordProtector wordProtector;

    @BeforeEach
    void setUp() {
        wordProtector = new DefaultWordProtector();
    }

    @Test
    void testBuildAndProtect_NullOrEmpty() {
        wordProtector.build(null);
        ProtectionResult result1 = wordProtector.protect("Test");
        assertEquals("Test", result1.maskedText());
        assertTrue(result1.replacements().isEmpty());

        wordProtector.build(Set.of("Böse"));
        ProtectionResult result2 = wordProtector.protect("");
        assertEquals("", result2.maskedText());
    }

    @Test
    void testProtect_CaseInsensitiveAndSingleCharacterIgnore() {
        wordProtector.build(Set.of("Apfel", "a", "Banane"));

        String input = "Ein Apfel und eine Banane sowie ein a.";
        ProtectionResult result = wordProtector.protect(input);

        assertTrue(result.maskedText().contains("{W0}"));
        assertTrue(result.maskedText().contains("{W1}"));
        assertTrue(result.maskedText().contains(" ein a."));
    }

    @Test
    void testRestore() {
        Map<String, String> replacements = Map.of(
                "{W0}", "Apfel",
                "{W1}", "Banane"
        );
        String input = "Ein {W0} und eine {W1}";

        String restored = wordProtector.restore(input, replacements);

        assertEquals("Ein Apfel und eine Banane", restored);
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testProtectAndRestore_RoundTrip_MultipleOccurrencesOfSameWord() {
        wordProtector.build(Set.of("Test", "Wort"));

        String input = "Das Test ist ein Test für das Wort Wort.";
        ProtectionResult result = wordProtector.protect(input);

        String restored = wordProtector.restore(result.maskedText(), result.replacements());

        assertEquals(input, restored);
    }

    @Test
    void testProtectAndRestore_RoundTrip_CaseInsensitiveMixedCase() {
        wordProtector.build(Set.of("Apfel"));

        String input = "APFEL, apfel und Apfel sind nicht alle gleich geschützt.";
        ProtectionResult result = wordProtector.protect(input);
        String restored = wordProtector.restore(result.maskedText(), result.replacements());

        assertEquals(input, restored);
        System.out.println(result.maskedText());
        assertTrue(result.maskedText().contains("apfel"));
        assertEquals(1, result.replacements().size());
    }

    @Test
    void testProtect_UserManuallyTypesPlaceholder_DoesNotCorrupt() {
        wordProtector.build(Set.of("Apfel"));

        String maliciousInput = "Ich esse einen Apfel und hacke {W0}";
        ProtectionResult result = wordProtector.protect(maliciousInput);

        assertFalse(result.replacements().isEmpty());
        assertTrue(result.maskedText().contains("{W"));
    }

    static Stream<Arguments> provideWordProtectionCases() {
        return Stream.of(
                Arguments.of(Set.of("Böse", "Schlecht"), "Das ist Böse und Schlecht", 2),
                Arguments.of(Set.of("Test"), "Nur ein Wort", 0),
                Arguments.of(Set.of("Apfel", "Birne"), "Das ist Apfel und Birne", 2)
        );
    }

    @ParameterizedTest
    @MethodSource("provideWordProtectionCases")
    void testProtect_DifferentBlacklists(Set<String> blacklist, String input, int expectedReplacements) {
        wordProtector.build(blacklist);
        var result = wordProtector.protect(input);

        assertEquals(expectedReplacements, result.replacements().size());
    }

    @Test
    void testRestore_MissingKeyInMap_LeavesPlaceholderIntact() {
        Map<String, String> incompleteReplacements = Map.of("{W0}", "Apfel");
        String input = "Hier ist ein {W0} und eine {W1}";

        String restored = wordProtector.restore(input, incompleteReplacements);

        // Sollte nicht crashen (NullPointerException) und das unaufgelöste {W1} stehen lassen
        assertEquals("Hier ist ein Apfel und eine {W1}", restored);
    }

    @Test
    void testProtectAndRestore_RoundTrip_WithPunctuation() {
        wordProtector.build(Set.of("Apfel", "Banane"));
        String input = "Apfel, Banane! Und nochmal Apfel?";

        ProtectionResult result = wordProtector.protect(input);
        String restored = wordProtector.restore(result.maskedText(), result.replacements());

        assertEquals(input, restored);
    }

    @Test
    void testProtectAndRestore_RoundTrip_WordAsSubstringOfOtherWords() {
        wordProtector.build(Set.of("Cat"));
        String input = "Cat and Catapult and concatenate";

        ProtectionResult result = wordProtector.protect(input);
        String restored = wordProtector.restore(result.maskedText(), result.replacements());

        assertEquals(input, restored);
    }

    @Test
    void testBuild_CalledAgain_ReplacesPreviousWordList() {
        wordProtector.build(Set.of("Apfel"));
        ProtectionResult first = wordProtector.protect("Ein Apfel");
        assertFalse(first.replacements().isEmpty());

        wordProtector.build(Set.of("Banane"));
        ProtectionResult second = wordProtector.protect("Ein Apfel");

        // "Apfel" ist nach dem Rebuild nicht mehr Teil der geschützten Wörter
        assertTrue(second.replacements().isEmpty());
    }

    @Test
    void testRestore_EmptyReplacements_ReturnsTextUnchanged() {
        String input = "Text ohne Platzhalter-Ersetzung nötig";
        String result = wordProtector.restore(input, Map.of());
        assertEquals(input, result);
    }

    @Test
    void testProtect_TwoCharacterWord_IsStillProtected() {
        // Nur EIN-Zeichen-Wörter werden ignoriert, ab 2 Zeichen wird geschützt
        wordProtector.build(Set.of("ok"));
        ProtectionResult result = wordProtector.protect("Das ist ok!");

        assertFalse(result.replacements().isEmpty());
        assertFalse(result.maskedText().toLowerCase().contains("ok"));
    }
}