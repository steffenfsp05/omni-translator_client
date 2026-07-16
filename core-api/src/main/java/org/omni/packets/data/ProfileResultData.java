package org.omni.packets.data;

import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public record ProfileResultData(
        UUID requestId,
        byte[] analyticId,
        Protobuf.ConsentType consentType
) {
    public static ProfileResultData createDefault(byte[] analyticId, UUID requestId) {

        return new ProfileResultData(
                requestId,
                analyticId,
                Protobuf.ConsentType.UNKNOWN
        );
    }

    public ProfileResultData withRequestId(UUID newRequestId) {
        return new ProfileResultData(
                newRequestId,
                this.analyticId,
                this.consentType
        );
    }


    public ProfileResultData withConsentType(Protobuf.ConsentType newConsent) {
        return new ProfileResultData(
                this.requestId,
                this.analyticId,
                newConsent
        );
    }

}

