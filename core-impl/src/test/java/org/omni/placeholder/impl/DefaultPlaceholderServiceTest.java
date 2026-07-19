package org.omni.placeholder.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omni.placeholder.BasePlaceholder;
import org.omni.placeholder.normalizer.PlaceholderNormalizer;
import org.omni.placeholder.protector.PlayerNameProtector;
import org.omni.placeholder.protector.ProtectionResult;
import org.omni.placeholder.protector.WordProtector;

import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPlaceholderServiceTest {

    @Mock private PlaceholderNormalizer normalizerMock;
    @Mock private PlayerNameProtector nameProtectorMock;
    @Mock private WordProtector wordProtectorMock;

    private DefaultPlaceholderService placeholderService;
    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        placeholderService = new DefaultPlaceholderService(normalizerMock, nameProtectorMock, wordProtectorMock);
    }

    @Test
    void testRegisterPlaceholder_PriorityConflict() {
        BasePlaceholder dummyPlaceholder = new ExtendedPlaceholder("DUMMY", () -> Pattern.compile("test"));

        boolean result = placeholderService.registerPlaceholder(1, dummyPlaceholder);
        assertFalse(result);

        boolean successResult = placeholderService.registerPlaceholder(99, dummyPlaceholder);
        assertTrue(successResult);
    }

    @Test
    void testToPlaceholders_EmptyInput() {

        assertNull(placeholderService.toPlaceholders(testUuid, null));
        assertEquals("", placeholderService.toPlaceholders(testUuid, ""));

        verifyNoInteractions(nameProtectorMock);
    }

    @Test
    void testToPlaceholders_FullPipeline() {
        String input = "Das Item kostet 10.50";
        ProtectionResult mockNameResult = mock(ProtectionResult.class);
        ProtectionResult mockWordResult = mock(ProtectionResult.class);

        when(mockNameResult.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(mockNameResult);

        when(mockWordResult.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect("Das Item kostet {N0}")).thenReturn(mockWordResult);

        when(normalizerMock.normalizeText(eq(testUuid), eq("Das Item kostet {N0}"))).thenReturn("Das Item kostet {N0}");

        String result = placeholderService.toPlaceholders(testUuid, input);

        assertEquals("Das Item kostet {N0}", result);

        verify(normalizerMock).normalizeText(eq(testUuid), anyString());
    }

    @Test
    void testFromPlaceholders_SystemProtectionSkip() {
        String input = "Tag {A0}";

        when(normalizerMock.denormalizeText(eq(testUuid), eq(input))).thenReturn(input);

        String result = placeholderService.fromPlaceholders(testUuid, input);

        assertEquals("Tag {A0}", result);
    }
    @Test
    void testToPlaceholders_ComplexIntegrationWithColors() {
        String input = "§aSteve verkauft 10.50 für §cSuperSword";

        ProtectionResult nameRes = new ProtectionResult("§a{P0} verkauft 10.50 für §cSuperSword",
                Collections.singletonMap("{P0}", "Steve"));
        when(nameProtectorMock.maskNames(anyString())).thenReturn(nameRes);

        ProtectionResult wordRes = new ProtectionResult("§a{P0} verkauft {N0} für §c{W0}",
                Collections.singletonMap("{W0}", "SuperSword"));
        when(wordProtectorMock.protect(anyString())).thenReturn(wordRes);

        when(normalizerMock.normalizeText(any(UUID.class), anyString()))
                .thenReturn("§a{P0} verkauft {N0} für §c{W0}");

        String result = placeholderService.toPlaceholders(testUuid, input);

        System.out.println("Result: " + result);
        assertEquals("§a{P0} verkauft {N0} für §c{W0}", result);

        verify(nameProtectorMock).maskNames(anyString());
        verify(wordProtectorMock).protect(anyString());
        verify(normalizerMock).normalizeText(any(UUID.class), anyString());
    }

    @Test
    void testFromPlaceholders_FullRestorationPipeline() {
        String masked = "§a{P0} verkauft {N0} für §c{W0}";

        when(normalizerMock.denormalizeText(eq(testUuid), anyString()))
                .thenReturn("§a{P0} verkauft 10.50 für §c{W0}");

        String result = placeholderService.fromPlaceholders(testUuid, masked);

        verify(normalizerMock).denormalizeText(eq(testUuid), eq(masked));
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testToPlaceholders_SkipsSystemProtectionTokens() {
        String input = "Text mit {A5} und [#TAG-1#] Marker";

        ProtectionResult emptyName = mock(ProtectionResult.class);
        when(emptyName.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(emptyName);

        ProtectionResult emptyWord = mock(ProtectionResult.class);
        when(emptyWord.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect(anyString())).thenReturn(emptyWord);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        String result = placeholderService.toPlaceholders(testUuid, input);

        // Bereits vorhandene Systemtokens dürfen nicht erneut verpackt werden
        assertEquals(input, result);
    }

    @Test
    void testToPlaceholders_ColorCodes_ConvertedToPlaceholders() {
        String input = "§aGrüner Text §#ff0000Roter Text";

        ProtectionResult emptyName = mock(ProtectionResult.class);
        when(emptyName.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(emptyName);

        ProtectionResult emptyWord = mock(ProtectionResult.class);
        when(emptyWord.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect(anyString())).thenReturn(emptyWord);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        String result = placeholderService.toPlaceholders(testUuid, input);

        assertTrue(result.contains("{C0}"));
        assertTrue(result.contains("{C1}"));
        assertTrue(result.contains("Grüner Text"));
        assertTrue(result.contains("Roter Text"));
    }

    @Test
    void testToPlaceholders_MultiplePrices_EachGetsDistinctId() {
        String input = "10 Äpfel kosten 5.99 und 10 Bananen kosten 3,50";

        ProtectionResult emptyName = mock(ProtectionResult.class);
        when(emptyName.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(emptyName);

        ProtectionResult emptyWord = mock(ProtectionResult.class);
        when(emptyWord.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect(anyString())).thenReturn(emptyWord);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        String result = placeholderService.toPlaceholders(testUuid, input);

        // "10" kommt zweimal vor, bekommt aber KEINE gemeinsame ID (kein Dedup)
        assertTrue(result.contains("{N0}"));
        assertTrue(result.contains("{N1}"));
        assertTrue(result.contains("{N2}"));
        assertTrue(result.contains("{N3}"));
    }

    @Test
    void testToAndFromPlaceholders_RoundTrip_NoProtectionNeeded() {
        String input = "Preis: 42.50 Rabatt: 10";

        ProtectionResult emptyName = mock(ProtectionResult.class);
        when(emptyName.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(emptyName);

        ProtectionResult emptyWord = mock(ProtectionResult.class);
        when(emptyWord.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect(anyString())).thenReturn(emptyWord);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(normalizerMock.denormalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        String masked = placeholderService.toPlaceholders(testUuid, input);
        assertEquals("Preis: {N0} Rabatt: {N1}", masked);

        String restored = placeholderService.fromPlaceholders(testUuid, masked);

        assertEquals(input, restored);
    }

    @Test
    void testFromPlaceholders_FullRoundTrip_WithNamesWordsAndPrices() {
        String input = "Steve verkauft SuperSword für 10.50";

        ProtectionResult nameRes = new ProtectionResult("{P0} verkauft SuperSword für 10.50",
                Collections.singletonMap("{P0}", "Steve"));
        when(nameProtectorMock.maskNames(input)).thenReturn(nameRes);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(normalizerMock.denormalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        ProtectionResult wordRes = new ProtectionResult("{P0} verkauft {W0} für {N0}",
                Collections.singletonMap("{W0}", "SuperSword"));
        when(wordProtectorMock.protect("{P0} verkauft SuperSword für {N0}")).thenReturn(wordRes);

        when(nameProtectorMock.restoreNames(anyString(), anyMap()))
                .thenAnswer(inv -> ((String) inv.getArgument(0)).replace("{P0}", "Steve"));
        when(wordProtectorMock.restore(anyString(), anyMap()))
                .thenAnswer(inv -> ((String) inv.getArgument(0)).replace("{W0}", "SuperSword"));

        String masked = placeholderService.toPlaceholders(testUuid, input);
        assertEquals("{P0} verkauft {W0} für {N0}", masked);

        String restored = placeholderService.fromPlaceholders(testUuid, masked);

        assertEquals(input, restored);
    }

    @Test
    void testFromPlaceholders_UnknownPlaceholderToken_RemainsUnchanged() {
        String input = "Wert: {N5}";

        when(normalizerMock.denormalizeText(eq(testUuid), eq(input))).thenReturn(input);

        String result = placeholderService.fromPlaceholders(testUuid, input);

        // Kein Cache-Eintrag vorhanden -> Token bleibt unangetastet
        assertEquals(input, result);
    }

    @Test
    void testRegisterPlaceholder_NewPlaceholder_IsUsedInToPlaceholders() {
        ExtendedPlaceholder customPlaceholder = new ExtendedPlaceholder("X", () -> Pattern.compile("FOO"));
        boolean registered = placeholderService.registerPlaceholder(50, customPlaceholder);
        assertTrue(registered);

        String input = "Hallo FOO Welt";

        ProtectionResult emptyName = mock(ProtectionResult.class);
        when(emptyName.replacements()).thenReturn(Collections.emptyMap());
        when(nameProtectorMock.maskNames(input)).thenReturn(emptyName);

        ProtectionResult emptyWord = mock(ProtectionResult.class);
        when(emptyWord.replacements()).thenReturn(Collections.emptyMap());
        when(wordProtectorMock.protect(anyString())).thenReturn(emptyWord);

        when(normalizerMock.normalizeText(eq(testUuid), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        String result = placeholderService.toPlaceholders(testUuid, input);

        assertEquals("Hallo {X0} Welt", result);
    }
}