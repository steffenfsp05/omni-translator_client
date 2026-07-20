package org.omni.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omni.placeholder.impl.*;
import org.omni.placeholder.protect.impl.DefaultPlayerNameProtector;
import org.omni.placeholder.protect.impl.DefaultWordProtector;
import org.omni.placeholder.protector.ProtectionResult;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PipelineRegexCollisionTest {

    private DefaultPlayerNameProtector nameProtector;
    private DefaultWordProtector wordProtector;
    private DefaultPlaceholderNormalizer normalizer;
    private DefaultPlaceholderService placeholderService;
    private final UUID testUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        nameProtector = new DefaultPlayerNameProtector();
        wordProtector = new DefaultWordProtector();
        normalizer = new DefaultPlaceholderNormalizer();
        placeholderService = new DefaultPlaceholderService(normalizer, nameProtector, wordProtector);
    }

    @Test
    void testRealRegexCollision_PlaceholdersIgnoreEachOther() {
        nameProtector.addPlayer("SuperSteve");
        wordProtector.build(java.util.Set.of("Item"));

        String originalInput = "SuperSteve kauft Item für 10.50";

        ProtectionResult nameRes = nameProtector.maskNames(originalInput);
        assertEquals("{P0} kauft Item für 10.50", nameRes.maskedText());

        ProtectionResult wordRes = wordProtector.protect(nameRes.maskedText());
        assertEquals("{P0} kauft {W0} für 10.50", wordRes.maskedText());

        String fullyMasked = placeholderService.toPlaceholders(testUuid, wordRes.maskedText());

        assertEquals("{P0} kauft {W0} für {N0}", fullyMasked);
    }
}