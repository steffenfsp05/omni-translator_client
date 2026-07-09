package org.omni.packets.data;

import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public record ProfileResultData(
        String license,
        byte[] analyticId,
        UUID requestId,
        Protobuf.ConsentType consentType
) {
    public static ProfileResultData createDefault(String license, byte[] analyticId, UUID requestId) {

        return new ProfileResultData(
                license,
                analyticId,
                requestId,
                Protobuf.ConsentType.UNKNOWN
        );
    }

    public ProfileResultData withRequestId(UUID newRequestId) {
        return new ProfileResultData(
                this.license,
                this.analyticId,
                newRequestId,
                this.consentType
        );
    }


    public ProfileResultData withConsentType(Protobuf.ConsentType newConsent) {
        return new ProfileResultData(
                this.license,
                this.analyticId,
                this.requestId,
                newConsent
        );
    }

}

