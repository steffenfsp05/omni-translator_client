package org.omni.packets.data;

import java.util.Map;
import java.util.UUID;

public record HeartBeatUpdateData(
        UUID requestId,
        long timestamp,
        int totalOnline,

        int translationUnknown,
        int translationExplicit,
        int translationAuto,
        int translationDeclined,

        int analyticsUnknown,
        int analyticsExplicit,
        int analyticsAuto,
        int analyticsDeclined,

        Map<String, Integer> languageDistribution
) {
}
