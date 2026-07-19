package org.omni.pipeline;

import org.junit.jupiter.api.Test;
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;

import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PipelineRegexCollisionTest {

    @Test
    void testOverlappingPatterns() {
        DefaultTranslationPipeline pipeline = new DefaultTranslationPipeline(List.of(
        ));

        UUID id = UUID.randomUUID();

        String input = "§#ff0000SuperSteve {10.50}";

        assertDoesNotThrow(() -> {
            String masked = pipeline.prepare(id, input);
            String restored = pipeline.restore(id, masked);

            assertEquals(input, restored, "Die Pipeline ist bei überlappenden Patterns gescheitert.");
        });
    }
}