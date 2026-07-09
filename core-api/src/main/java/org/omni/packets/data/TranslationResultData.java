package org.omni.packets.data;

import java.util.UUID;

public record TranslationResultData(UUID requestId, String result) {
}
