package org.omni.packets.data;

import java.util.Map;
import java.util.UUID;

public record HeartBeatUpdateData(
        String license,
        UUID requestId,
        Long timestamp,
        int total_online,
        int consent_unknown,
        int consent_explicit,
        int consent_auto,
        int consent_declined,
        Map<String,Integer> language_distribution
) {
}
