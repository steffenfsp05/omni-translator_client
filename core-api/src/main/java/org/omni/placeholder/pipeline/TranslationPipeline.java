package org.omni.placeholder.pipeline;

import java.util.UUID;

public interface TranslationPipeline {
    String prepare(UUID id, String text);
    String restore(UUID id, String text);
}
