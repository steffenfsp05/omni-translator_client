package org.omni.placeholder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omni.placeholder.impl.ExtendedPlaceholder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedPlaceholderTest {

    private ExtendedPlaceholder placeholder;
    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        placeholder = new ExtendedPlaceholder("TEST", () -> Pattern.compile("\\d+"));
    }

    @Test
    void testPlaceholder_ReturnsConfiguredKey() {
        assertEquals("TEST", placeholder.placeholder());
    }

    @Test
    void testGetPattern_ReturnsPatternFromSupplier() {
        assertEquals("\\d+", placeholder.getPattern().pattern());
    }

    @Test
    void testGetPattern_CallsSupplierEveryTime_ReflectsChanges() {
        // Da getPattern() den Supplier bei JEDEM Aufruf erneut ausführt,
        // muss eine Änderung am zugrunde liegenden Pattern sofort sichtbar sein
        AtomicReference<Pattern> currentPattern = new AtomicReference<>(Pattern.compile("A"));
        ExtendedPlaceholder dynamicPlaceholder = new ExtendedPlaceholder("DYN", currentPattern::get);

        assertEquals("A", dynamicPlaceholder.getPattern().pattern());

        currentPattern.set(Pattern.compile("B"));
        assertEquals("B", dynamicPlaceholder.getPattern().pattern());
    }

    @Test
    void testCachedValues_ReturnsSameCacheInstanceAcrossCalls() {
        assertSame(placeholder.cachedValues(), placeholder.cachedValues());
    }

    @Test
    void testCachedValues_IsInitiallyEmptyForUnknownUuid() {
        assertNull(placeholder.cachedValues().getIfPresent(testUuid));
    }

    @Test
    void testCachedValues_StoresAndRetrievesData() {
        Map<Integer, String> values = Map.of(0, "Hello");
        placeholder.cachedValues().put(testUuid, values);

        assertEquals(values, placeholder.cachedValues().getIfPresent(testUuid));
    }

    @Test
    void testCachedValues_IsolatedPerPlaceholderInstance() {
        ExtendedPlaceholder other = new ExtendedPlaceholder("OTHER", () -> Pattern.compile("."));

        placeholder.cachedValues().put(testUuid, Map.of(0, "Value A"));

        // Jede Placeholder-Instanz hat ihren eigenen Cache
        assertNull(other.cachedValues().getIfPresent(testUuid));
    }
}