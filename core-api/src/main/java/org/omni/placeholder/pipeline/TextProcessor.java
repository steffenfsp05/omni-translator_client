package org.omni.placeholder.pipeline;

import java.util.UUID;

public interface TextProcessor {
        String process(UUID id, String text);
        String restore(UUID id, String text);
}
