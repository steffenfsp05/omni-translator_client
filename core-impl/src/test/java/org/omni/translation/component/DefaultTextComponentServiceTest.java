package org.omni.translation.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omni.entity.TranslationModule;
import org.omni.translation.TranslatorService;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTextComponentServiceTest {

    @Mock
    private TranslatorService translatorServiceMock;

    private DefaultTextComponentService service;

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @BeforeEach
    void setUp() {
        service = new DefaultTextComponentService(translatorServiceMock);
    }

    @Test
    void testSanitizeLegacyText() {
        assertEquals("Hallo Welt", service.sanitizeLegacyText("Hallo Welt"));

        assertEquals("§aTest", service.sanitizeLegacyText("§aTest§"));

        assertEquals("Farbe", service.sanitizeLegacyText("Farbe§"));

        assertEquals("", service.sanitizeLegacyText(null));
    }

    @Test
    void testCacheOperations() {
        TextComponentService.TranslationKey key = mock(TextComponentService.TranslationKey.class);
        Component comp = Component.text("Test Component");

        assertFalse(service.exists(key));

        service.set(key, comp);
        assertTrue(service.exists(key));
        assertEquals(comp, service.get(key));

        service.invalidate(key);
        assertFalse(service.exists(key));
    }

    @Test
    void testTranslateComplexMessage() {
        Component originalText = Component.text("Hello World");
        String targetLang = "de";
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), eq(targetLang), eq(module)))
                .thenReturn(CompletableFuture.completedFuture("Hallo Welt"));

        CompletableFuture<Component> future = service.translateComplexMessage(originalText, targetLang, module);
        Component result = future.join();

        assertNotNull(result);

        String legacyText = serializer.serialize(result);

        String plainText = service.sanitizeLegacyText(legacyText);

        assertTrue(plainText.contains("Hallo Welt"));

        verify(translatorServiceMock, times(1)).translate(anyString(), eq(targetLang), eq(module));
    }

    @Test
    void testTranslateComplexMessage_WithNestedComponents() {
        Component original = Component.text("Hello", net.kyori.adventure.text.format.NamedTextColor.RED)
                .append(Component.text(" World", net.kyori.adventure.text.format.NamedTextColor.BLUE));

        String targetLang = "de";
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), eq(targetLang), eq(module)))
                .thenReturn(CompletableFuture.completedFuture("Hallo Welt"));

        CompletableFuture<Component> future = service.translateComplexMessage(original, targetLang, module);
        Component result = future.join();

        String plainText = serializer.serialize(result);
        assertTrue(plainText.contains("Hallo Welt"));

        verify(translatorServiceMock, times(1)).translate(anyString(), eq(targetLang), eq(module));
    }

    @Test
    void testTranslateComplexMessage_HandlesAsyncException() {
        Component original = Component.text("ErrorTest");
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), anyString(), eq(module)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Translation Service Down")));

        CompletableFuture<Component> future = service.translateComplexMessage(original, "de", module);

        assertFalse(future.isCompletedExceptionally(), "Der Future sollte bei einem Fehler nicht abbrechen, sondern den Fallback liefern");

        Component result = future.join();

        assertEquals(original, result, "Bei Fehler sollte der Original-Text zurückgegeben werden");
    }

    @Test
    void testCacheOperations_PersistenceIntegrity() {
        TextComponentService.TranslationKey key1 = mock(TextComponentService.TranslationKey.class);
        TextComponentService.TranslationKey key2 = mock(TextComponentService.TranslationKey.class);
        Component comp1 = Component.text("Component 1");
        Component comp2 = Component.text("Component 2");

        service.set(key1, comp1);
        service.set(key2, comp2);

        assertEquals(comp1, service.get(key1));
        assertEquals(comp2, service.get(key2));

        service.invalidate(key1);
        assertFalse(service.exists(key1));
        assertTrue(service.exists(key2));
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testSanitizeLegacyText_InvalidCodeLetterIsRemoved() {
        // 'z' ist kein gültiger Farb-/Format-Code -> das § davor wird entfernt
        assertEquals("z", service.sanitizeLegacyText("§z"));
    }

    @Test
    void testSanitizeLegacyText_DoubleSectionSign() {
        // Das erste § steht vor einem weiteren § (ungültig) -> wird entfernt,
        // das zweite § vor 'a' (gültig) bleibt erhalten
        assertEquals("§a", service.sanitizeLegacyText("§§a"));
    }

    @Test
    void testSanitizeLegacyText_ValidHexColorIsPreserved() {
        assertEquals("§#ff0000Text", service.sanitizeLegacyText("§#ff0000Text"));
    }

    @Test
    void testSanitizeLegacyText_OnlySectionSign_ResultsInEmptyString() {
        assertEquals("", service.sanitizeLegacyText("§"));
    }

    @Test
    void testGet_ReturnsNullForUnknownKey() {
        TextComponentService.TranslationKey key = mock(TextComponentService.TranslationKey.class);
        assertNull(service.get(key));
    }

    @Test
    void testClear_RemovesAllCachedEntries() {
        TextComponentService.TranslationKey key1 = mock(TextComponentService.TranslationKey.class);
        TextComponentService.TranslationKey key2 = mock(TextComponentService.TranslationKey.class);

        service.set(key1, Component.text("A"));
        service.set(key2, Component.text("B"));

        service.clear();

        assertFalse(service.exists(key1));
        assertFalse(service.exists(key2));
    }

    @Test
    void testTranslateComplexMessage_EqualComponents_HitCache() {
        String targetLang = "de";
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), eq(targetLang), eq(module)))
                .thenReturn(CompletableFuture.completedFuture("Hallo Welt"));

        // Zwei strukturell gleiche, aber unterschiedliche Component-Instanzen
        Component first = Component.text("Hello World");
        Component second = Component.text("Hello World");

        service.translateComplexMessage(first, targetLang, module).join();
        service.translateComplexMessage(second, targetLang, module).join();

        // Der zweite Aufruf sollte aus dem Cache kommen, nicht erneut übersetzen
        verify(translatorServiceMock, times(1)).translate(anyString(), eq(targetLang), eq(module));
    }

    @Test
    void testTranslateComplexMessage_DifferentLanguages_AreNotCachedTogether() {
        Component original = Component.text("Hello");
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), anyString(), eq(module)))
                .thenReturn(CompletableFuture.completedFuture("Übersetzt"));

        service.translateComplexMessage(original, "de", module).join();
        service.translateComplexMessage(original, "fr", module).join();

        verify(translatorServiceMock, times(2)).translate(anyString(), anyString(), eq(module));
    }

    @Test
    void testTranslateComplexMessage_EmptyComponent_CompletesWithoutError() {
        Component empty = Component.empty();
        TranslationModule module = mock(TranslationModule.class);

        when(translatorServiceMock.translate(anyString(), anyString(), eq(module)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        Component result = service.translateComplexMessage(empty, "de", module).join();

        assertNotNull(result);
        assertEquals("", service.sanitizeLegacyText(serializer.serialize(result)));
    }

    @Test
    void testTranslateComplexMessage_PreservesClickEventOnDescendant() {
        ClickEvent click = ClickEvent.runCommand("/help");
        Component original = Component.text("Click me").clickEvent(click);
        TranslationModule module = mock(TranslationModule.class);

        // Echo: der "übersetzte" Text bleibt unverändert
        when(translatorServiceMock.translate(anyString(), eq("de"), eq(module)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        Component result = service.translateComplexMessage(original, "de", module).join();

        assertTrue(containsClickEvent(result, click));
    }

    @Test
    void testTranslateComplexMessage_HoverTranslationFails_FallsBackToOriginalHoverText() {
        HoverEvent<Component> hover = HoverEvent.showText(Component.text("Hover Original"));
        Component original = Component.text("Main").hoverEvent(hover);
        TranslationModule module = mock(TranslationModule.class);

        // Haupttext-Übersetzung funktioniert (Echo)
        when(translatorServiceMock.translate(argThat(s -> s != null && !s.contains("Hover")), eq("de"), eq(module)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        // Hover-Übersetzung schlägt fehl
        when(translatorServiceMock.translate(argThat(s -> s != null && s.contains("Hover")), eq("de"), eq(module)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Hover service down")));

        // Darf trotz fehlgeschlagener Hover-Übersetzung nicht exceptionally enden
        Component result = service.translateComplexMessage(original, "de", module).join();

        assertNotNull(result);
        assertEquals("Hover Original", findHoverPlainText(result));
    }

    private boolean containsClickEvent(Component component, ClickEvent target) {
        if (target.equals(component.clickEvent())) return true;
        for (Component child : component.children()) {
            if (containsClickEvent(child, target)) return true;
        }
        return false;
    }

    private String findHoverPlainText(Component component) {
        HoverEvent<?> hover = component.hoverEvent();
        if (hover != null && hover.value() instanceof Component hoverComp) {
            return serializer.serialize(hoverComp);
        }
        for (Component child : component.children()) {
            String found = findHoverPlainText(child);
            if (found != null) return found;
        }
        return null;
    }
}