package org.omni.packets.data;

import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public record ConsentRefreshRequestData(
        UUID requestId,
        UUID playerId,
        Protobuf.ConsentType translationConsentType,
        Protobuf.ConsentType analyticConsentType
) {

}
