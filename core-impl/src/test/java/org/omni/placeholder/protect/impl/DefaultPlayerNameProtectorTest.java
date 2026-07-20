package org.omni.placeholder.protect.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omni.placeholder.protector.ProtectionResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPlayerNameProtectorTest {

    private DefaultPlayerNameProtector nameProtector;

    @BeforeEach
    void setUp() {
        nameProtector = new DefaultPlayerNameProtector();
    }

    @Test
    void testMaskNames_NoOnlinePlayers() {
        ProtectionResult result = nameProtector.maskNames("Hallo Notch");
        assertEquals("Hallo Notch", result.maskedText());
        assertTrue(result.replacements().isEmpty());
    }

    @Test
    void testRestoreNames_MissingKeyInMap_LeavesPlaceholderIntact() {
        Map<String, String> incompleteReplacements = Map.of("{P0}", "Steve");
        String input = "Hallo {P0} und {P1}!";

        String restored = nameProtector.restoreNames(input, incompleteReplacements);

        assertEquals("Hallo Steve und {P1}!", restored);
    }

    @Test
    void testMaskNames_UserManuallyTypesPlaceholder_IsIgnoredByRegex() {
        nameProtector.addPlayer("Steve");
        String input = "Steve {P0} {P1}";

        ProtectionResult result = nameProtector.maskNames(input);

        assertEquals(1, result.replacements().size());
        assertTrue(result.maskedText().contains("{P0}"));
    }

    @Test
    void testMaskNames_WithColorsAndCaseInsensitive() {
        nameProtector.addPlayer("Zelmyra");
        nameProtector.addPlayer("Steve");

        String input = "Hey §aZELMYRA und &cSteve!";
        ProtectionResult result = nameProtector.maskNames(input);

        assertTrue(result.maskedText().contains("§a{P0}"));
        assertTrue(result.maskedText().contains("&c{P1}"));

        assertEquals("ZELMYRA", result.replacements().get("{P0}"));
        assertEquals("Steve", result.replacements().get("{P1}"));
    }

    @Test
    void testAddAndRemovePlayer() {
        nameProtector.addPlayer("Alex");
        assertEquals(1, nameProtector.maskNames("Alex").replacements().size());

        nameProtector.removePlayer("Alex");
        assertEquals(0, nameProtector.maskNames("Alex").replacements().size());
    }

    @Test
    void testRestoreNames() {
        Map<String, String> replacements = Map.of("{P0}", "Zelmyra");
        String restored = nameProtector.restoreNames("Hallo §a{P0}", replacements);

        assertEquals("Hallo §aZelmyra", restored);
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testMaskAndRestore_RoundTrip() {
        nameProtector.addPlayer("Notch");
        nameProtector.addPlayer("Zelmyra");

        String input = "Hallo §aNotch und §bZELMYRA, wie geht's?";
        ProtectionResult result = nameProtector.maskNames(input);

        String restored = nameProtector.restoreNames(result.maskedText(), result.replacements());

        assertEquals(input, restored);
    }

    @Test
    void testMaskAndRestore_RoundTrip_RepeatedNameInText() {
        nameProtector.addPlayer("Notch");

        String input = "Notch traf Notch im Wald.";
        ProtectionResult result = nameProtector.maskNames(input);
        String restored = nameProtector.restoreNames(result.maskedText(), result.replacements());

        assertEquals(input, restored);
    }

    @Test
    void testMaskNames_MultiplePlayers_EachGetsOwnPlaceholder() {
        nameProtector.addPlayer("Alex");
        nameProtector.addPlayer("Steve");
        nameProtector.addPlayer("Notch");

        String input = "Alex, Steve und Notch spielen zusammen.";
        ProtectionResult result = nameProtector.maskNames(input);

        assertEquals(3, result.replacements().size());
        assertEquals(input, nameProtector.restoreNames(result.maskedText(), result.replacements()));
    }

    @Test
    void testMaskNames_EmptyInput_ReturnsEmptyResult() {
        nameProtector.addPlayer("Notch");
        ProtectionResult result = nameProtector.maskNames("");

        assertEquals("", result.maskedText());
        assertTrue(result.replacements().isEmpty());
    }

    @Test
    void testRemovePlayer_NeverAdded_DoesNotThrow() {
        assertDoesNotThrow(() -> nameProtector.removePlayer("Ghost"));
    }

    @Test
    void testRestoreNames_EmptyReplacements_ReturnsTextUnchanged() {
        String input = "Hallo ohne Platzhalter";
        String result = nameProtector.restoreNames(input, Map.of());

        assertEquals(input, result);
    }
}