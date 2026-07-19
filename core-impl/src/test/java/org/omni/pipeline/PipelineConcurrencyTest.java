package org.omni.pipeline;

import org.junit.jupiter.api.Test;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class PipelineConcurrencyTest {

    @Test
    void testParallelPipelineExecution() throws InterruptedException, ExecutionException {
        DefaultTranslationPipeline pipeline = new DefaultTranslationPipeline(List.of(new MockProcessor("P1"), new MockProcessor("P2")));

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);

        Callable<Boolean> task = () -> {
            UUID id = UUID.randomUUID();
            String input = "Text für " + id;
            String prepared = pipeline.prepare(id, input);
            String restored = pipeline.restore(id, prepared);
            return input.equals(restored);
        };

        var futures = IntStream.range(0, threadCount)
                .mapToObj(i -> executor.submit(task))
                .toList();

        for (var f : futures) {
            assertTrue(f.get(), "Thread-Isolation fehlgeschlagen: Daten-Vermischung zwischen UUIDs.");
        }

        executor.shutdown();
    }

    private record MockProcessor(String prefix) implements TextProcessor {
        public String process(UUID id, String text) {
            return prefix + text;
        }

        public String restore(UUID id, String text) {
            return text.substring(prefix.length());
        }
        }
}