package org.omni.packets.data;

import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public record ProfileResultData(
        UUID requestId,
        byte[] analyticId,
        Protobuf.ConsentType translationConsent,
        Protobuf.ConsentType analyticConsent,
        long analyticsAcceptedTimestamp,
        long translationsAcceptedTimestamp

) {
    public static ProfileResultData createDefault(byte[] analyticId, UUID requestId) {

        return new ProfileResultData(
                requestId,
                analyticId,
                Protobuf.ConsentType.UNKNOWN,
                Protobuf.ConsentType.UNKNOWN,
                0,
                0
        );
    }

    public ProfileResultData withRequestId(UUID newRequestId) {
        return new ProfileResultData(
                newRequestId,
                this.analyticId,
                this.translationConsent,
                this.analyticConsent,
                this.analyticsAcceptedTimestamp,
                this.translationsAcceptedTimestamp
        );
    }


    public ProfileResultData withTranslationConsentType(Protobuf.ConsentType newTranslationConsent) {
        return new ProfileResultData(
                this.requestId,
                this.analyticId,
                newTranslationConsent,
                this.analyticConsent,
                this.analyticsAcceptedTimestamp,
                (newTranslationConsent == Protobuf.ConsentType.EXPLICIT ? System.currentTimeMillis() : this.translationsAcceptedTimestamp)
        );
    }
    public ProfileResultData withAnalyticConsentType(Protobuf.ConsentType newAnalyticConsent) {
        return new ProfileResultData(
                this.requestId,
                this.analyticId,
                this.translationConsent,
                newAnalyticConsent,
                (newAnalyticConsent == Protobuf.ConsentType.EXPLICIT ? System.currentTimeMillis() : this.analyticsAcceptedTimestamp),
                this.translationsAcceptedTimestamp
        );
    }

}

