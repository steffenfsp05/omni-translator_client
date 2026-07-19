package org.omni.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omni.placeholder.pipeline.TextProcessor;
import org.mockito.InOrder;
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;

import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PipelineIntegrationTest {

    private DefaultTranslationPipeline pipeline;
    private TextProcessor gradientMock;
    private TextProcessor nameMock;
    private TextProcessor placeholderMock;

    @BeforeEach
    void setUp() {
        gradientMock = mock(TextProcessor.class);
        nameMock = mock(TextProcessor.class);
        placeholderMock = mock(TextProcessor.class);

        pipeline = new DefaultTranslationPipeline(List.of(gradientMock, nameMock, placeholderMock));
    }

    @Test
    void testFullRoundTripConsistency() {
        UUID id = UUID.randomUUID();
        String originalText = "§#ff0000Steve verkauft SuperSword";

        when(gradientMock.process(eq(id), anyString())).thenReturn("Steve verkauft SuperSword");
        when(nameMock.process(eq(id), eq("Steve verkauft SuperSword"))).thenReturn("{P0} verkauft SuperSword");
        when(placeholderMock.process(eq(id), eq("{P0} verkauft SuperSword"))).thenReturn("{P0} verkauft {W0}");

        when(placeholderMock.restore(eq(id), anyString())).thenAnswer(i -> i.getArgument(1).toString().replace("{W0}", "SuperSword"));
        when(nameMock.restore(eq(id), anyString())).thenAnswer(i -> i.getArgument(1).toString().replace("{P0}", "Steve"));
        when(gradientMock.restore(eq(id), anyString())).thenAnswer(i -> "§#ff0000" + i.getArgument(1));

        String masked = pipeline.prepare(id, originalText);
        String restored = pipeline.restore(id, masked);

        InOrder inOrder = inOrder(gradientMock, nameMock, placeholderMock);
        inOrder.verify(gradientMock).process(id, originalText);
        inOrder.verify(nameMock).process(id, "Steve verkauft SuperSword");
        inOrder.verify(placeholderMock).process(id, "{P0} verkauft SuperSword");

        assertEquals(originalText, restored, "Die Pipeline muss das Original exakt wiederherstellen.");
    }
}