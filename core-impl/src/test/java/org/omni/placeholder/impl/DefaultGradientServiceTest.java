package org.omni.placeholder.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omni.placeholder.gradient.ExtractionResult;
import org.omni.placeholder.gradient.GradientData;
import org.omni.placeholder.gradient.GradientService;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGradientServiceTest {

    private GradientService gradientService;
    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        gradientService = new DefaultGradientService();
    }

    @Test
    void testStripAndAnalyze_NullOrEmpty() {
        ExtractionResult resultNull = gradientService.stripAndAnalyze(null);
        assertNull(resultNull.cleanText());
        assertTrue(resultNull.gradients().isEmpty());

        ExtractionResult resultEmpty = gradientService.stripAndAnalyze("");
        assertEquals("", resultEmpty.cleanText());
    }

    @Test
    void testStripAndAnalyze_NoGradients() {
        // Normaler Text ohne Farbcodes
        String input = "Hallo Welt ohne Farben";
        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertEquals(input, result.cleanText());
        assertTrue(result.gradients().isEmpty());
    }

    @Test
    void testStripAndAnalyze_FullLine() {
        String input = "§#ff0000H§#00ff00i";
        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertEquals("Hi", result.cleanText());
        assertTrue(result.gradients().containsKey("FULL_LINE"));

        GradientData data = result.gradients().get("FULL_LINE");
        assertEquals(new Color(0xff0000), data.startColor());
        assertEquals(new Color(0x00ff00), data.endColor());
    }

    @Test
    void testStripAndAnalyze_PartialWords() {
        String input = "Das ist ein §#ff0000T§#00ff00est Wort";
        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertTrue(result.cleanText().contains("<G0>Test Wort</G0>"));
        assertTrue(result.gradients().containsKey("G0"));
    }

    @Test
    void testRestoreGradients_MissingCache() {
        String input = "Hallo <G0>Welt</G0>";
        String result = gradientService.restoreGradients(testUuid, input);

        assertEquals(input, result);
    }

    @Test
    void testRestoreGradients_OneCharacterWord() {
        Map<String, GradientData> mockCache = new HashMap<>();
        mockCache.put("G0", new GradientData(Color.RED, Color.BLUE, false, false));
        gradientService.cacheGradient(testUuid, mockCache);

        String input = "Ein <G0>X</G0> im Text";
        String result = gradientService.restoreGradients(testUuid, input);

        assertTrue(result.contains("§#ff0000X"));
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testStripAndAnalyze_SingleColorCode_IsNotTreatedAsGradient() {
        // Nur EIN Farbcode -> Pattern verlangt mindestens 2 Wiederholungen
        String input = "§#ff0000Hello";

        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertTrue(result.gradients().isEmpty());
        assertEquals(input, result.cleanText());
    }



    @Test
    void testStripAndAnalyze_BoldAndItalicFlagsAreDetected() {
        String input = "§#ff0000§l§oB§#00ff00old";

        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertTrue(result.gradients().containsKey("FULL_LINE"));
        GradientData data = result.gradients().get("FULL_LINE");
        assertTrue(data.bold());
        assertTrue(data.italic());
    }

    @Test
    void testStripAndAnalyze_SolidColorFullLine_UsesSingleColorPrefix() {
        String input = "§#ff0000S§#ff0000olid";

        ExtractionResult result = gradientService.stripAndAnalyze(input);
        assertTrue(result.gradients().containsKey("FULL_LINE"));

        gradientService.cacheGradient(testUuid, result.gradients());
        String restored = gradientService.restoreGradients(testUuid, result.cleanText());

        // Start- und Endfarbe sind identisch -> keine Interpolation, nur ein Farbpräfix
        assertEquals("§#ff0000Solid", restored);
    }

    @Test
    void testRestoreGradients_TagIdNotInCache_RemainsAsRawTag() {
        Map<String, GradientData> cache = new HashMap<>();
        cache.put("G0", new GradientData(Color.RED, Color.BLUE, false, false));
        gradientService.cacheGradient(testUuid, cache);

        String input = "<G0>Rot-Blau</G0> und <G1>Unbekannt</G1>";
        String result = gradientService.restoreGradients(testUuid, input);

        assertFalse(result.contains("<G0>"));
        assertTrue(result.contains("<G1>Unbekannt</G1>"));
    }

    @Test
    void testStripAndAnalyze_LegacyPerCharacterHexColorFormat() {
        String input = "§x§f§f§0§0§0§0H§x§0§0§f§f§0§0i";

        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertEquals("Hi", result.cleanText());
        assertTrue(result.gradients().containsKey("FULL_LINE"));

        GradientData data = result.gradients().get("FULL_LINE");
        assertEquals(new Color(0xff0000), data.startColor());
        assertEquals(new Color(0x00ff00), data.endColor());
    }

    @Test
    void testRestoreGradients_TwoCharacterWord_StartAndEndColorsAreExact() {
        Map<String, GradientData> cache = new HashMap<>();
        cache.put("G0", new GradientData(Color.RED, Color.BLUE, false, false));
        gradientService.cacheGradient(testUuid, cache);

        String input = "<G0>AB</G0>";
        String result = gradientService.restoreGradients(testUuid, input);

        assertTrue(result.startsWith(toHex(Color.RED) + "A"));
        assertTrue(result.contains(toHex(Color.BLUE) + "B"));
    }

    @Test
    void testCacheGradient_GetAndInvalidate_DirectRoundTrip() {
        Map<String, GradientData> data = Collections.singletonMap("G0",
                new GradientData(Color.RED, Color.GREEN, true, false));

        assertNull(gradientService.getCachedGradient(testUuid));

        gradientService.cacheGradient(testUuid, data);
        assertEquals(data, gradientService.getCachedGradient(testUuid));

        gradientService.invalidCachedGradient(testUuid);
        assertNull(gradientService.getCachedGradient(testUuid));
    }

    @Test
    void testStripAndAnalyze_MultiStopGradient_UsesFirstAndLastColorOnly() {
        String input = "§#ff0000R§#00ff00G§#0000ffB";

        ExtractionResult result = gradientService.stripAndAnalyze(input);

        assertEquals("RGB", result.cleanText());
        GradientData data = result.gradients().get("FULL_LINE");

        assertEquals(new Color(0xff0000), data.startColor());
        assertEquals(new Color(0x0000ff), data.endColor());
    }

    private String toHex(Color c) {
        return String.format("§#%06x", c.getRGB() & 0xFFFFFF);
    }
}