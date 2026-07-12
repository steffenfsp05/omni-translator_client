package org.omni.packets.data;

import java.util.UUID;

public record ProfileInternRequestData(
        UUID playerId,
        UUID requestId
) {

}
