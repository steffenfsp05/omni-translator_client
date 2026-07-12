package org.omni.packets.data;

import java.util.UUID;

public record TrackPlayerRequestData(
        String licenseKey,
        UUID requestId,
        byte[] analyticId,
        long timestamp,
        int playtimeSeconds,
        boolean is_translated,
        String language) {


}
