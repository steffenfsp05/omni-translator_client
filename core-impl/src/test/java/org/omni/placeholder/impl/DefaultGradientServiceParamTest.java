package org.omni.placeholder.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.omni.placeholder.gradient.ExtractionResult;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultGradientServiceParamTest {

    private final DefaultGradientService gradientService = new DefaultGradientService();

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Normaler Text",
            "§aNur eine Farbe",
            "§#ff0000Unvollständiger Gradient"
    })
    void testStripAndAnalyze_NoGradientsDetected(String input) {
        ExtractionResult result = gradientService.stripAndAnalyze(input);

        // Sollte bei diesen Inputs keine Gradienten-Keys in der Map haben
        assertTrue(result.gradients().isEmpty(),
                "Gradienten-Map sollte leer sein für Input: " + input);
    }

    static Stream<Arguments> provideGradientInput() {
        return Stream.of(
                Arguments.of("§#ff0000Start§#00ff00End", true),
                Arguments.of("§#ff0000NurEineFarbe", false),
                Arguments.of("Text ohne Code", false),
                Arguments.of("§x§f§f§0§0§0§0Test§x§0§0§f§f§0§0", true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideGradientInput")
    void testStripAndAnalyze_GradientValidation(String input, boolean shouldHaveGradient) {
        var result = gradientService.stripAndAnalyze(input);

        if (shouldHaveGradient) {
            assertFalse(result.gradients().isEmpty(), "Sollte Gradienten enthalten: " + input);
        } else {
            assertTrue(result.gradients().isEmpty(), "Sollte leer sein: " + input);
        }
    }
}