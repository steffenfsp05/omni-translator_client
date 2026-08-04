package org.pytenix.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.Pair;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GDPRService {

    @Inject
    final ProfileEndpoint profileEndpoint;


    @Inject
    public GDPRService(ProfileEndpoint profileEndpoint)
    {
        this.profileEndpoint = profileEndpoint;
    }
    public CompletableFuture<Boolean> needGDPRScreen(UUID playerId)
    {

        return profileEndpoint.sendRequest(playerId).thenApply(profileResultData ->
                profileResultData.translationConsent() == Protobuf.ConsentType.UNKNOWN &&
                        profileResultData.analyticConsent() == Protobuf.ConsentType.UNKNOWN
                );

    }

    public CompletableFuture<Void> setConsents(UUID playerId, boolean translationConsent, boolean analyticConsent)
    {

        return profileEndpoint.sendRequest(playerId).thenAcceptAsync(profileResultData ->
        {

            profileResultData = profileResultData.withTranslationConsentType(translationConsent ? Protobuf.ConsentType.EXPLICIT : Protobuf.ConsentType.DECLINED);
            profileResultData = profileResultData.withAnalyticConsentType(analyticConsent ? Protobuf.ConsentType.EXPLICIT : Protobuf.ConsentType.DECLINED);

            profileEndpoint.update(profileResultData);
        });

    }

}
