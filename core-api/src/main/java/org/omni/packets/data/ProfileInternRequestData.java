package org.omni.packets.data;

import java.util.UUID;

public record ProfileInternRequestData(
        String license,
        UUID playerId,
        UUID requestId
) {

}
