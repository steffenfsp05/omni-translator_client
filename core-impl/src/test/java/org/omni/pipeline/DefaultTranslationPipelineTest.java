package org.omni.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;

import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;

class DefaultTranslationPipelineTest {

    private TextProcessor p1;
    private TextProcessor p2;
    private DefaultTranslationPipeline pipeline;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        p1 = mock(TextProcessor.class);
        p2 = mock(TextProcessor.class);
        pipeline = new DefaultTranslationPipeline(List.of(p1, p2));
    }

    @Test
    void testProcessOrder() {
        pipeline.prepare(id, "text");

        InOrder inOrder = inOrder(p1, p2);
        inOrder.verify(p1).process(eq(id), any());
        inOrder.verify(p2).process(eq(id), any());
    }

    @Test
    void testRestoreOrder() {
        pipeline.restore(id, "text");

        InOrder inOrder = inOrder(p1, p2);
        inOrder.verify(p2).restore(eq(id), any());
        inOrder.verify(p1).restore(eq(id), any());
    }
}