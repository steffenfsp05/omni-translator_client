package org.omni.placeholder.protect.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.omni.placeholder.protector.ProtectionResult;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlayerNameProtectorParamTest {

    private final DefaultPlayerNameProtector nameProtector = new DefaultPlayerNameProtector();

    static Stream<Arguments> provideNameCases() {
        return Stream.of(
                Arguments.of("§aNotch", "Notch"),
                Arguments.of("Hallo §bSteve!", "Steve"),
                Arguments.of("§1§lZelmyra", "Zelmyra")
        );
    }

    @ParameterizedTest
    @MethodSource("provideNameCases")
    void testMaskNames_VariousFormats(String input, String nameToRegister) {
        nameProtector.addPlayer(nameToRegister);
        ProtectionResult result = nameProtector.maskNames(input);

        assertTrue(result.maskedText().contains("{P"), "Masked text missing placeholder for: " + input);
        assertTrue(result.replacements().containsValue(nameToRegister));
    }
}