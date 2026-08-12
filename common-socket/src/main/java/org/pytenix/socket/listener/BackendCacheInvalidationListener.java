package org.pytenix.socket.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.TranslationModule;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.translation.component.TextComponentService;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.omni.transport.endpoint.TranslationEndpoint;
import org.omni.util.SignalOperations;

import java.util.Base64;
import java.util.UUID;

@Singleton
public class BackendCacheInvalidationListener {


    final AbstractAnalyticsSecret abstractAnalyticsSecret;
    final ProfileEndpoint profileEndpoint;
    final TranslationEndpoint translationEndpoint;
    final TextComponentService textComponentService;

    @Inject
    public BackendCacheInvalidationListener(AbstractAnalyticsSecret abstractAnalyticsSecret, ProfileEndpoint profileEndpoint, TranslationEndpoint translationEndpoint,
                                            TextComponentService textComponentService) {
        this.profileEndpoint = profileEndpoint;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.translationEndpoint = translationEndpoint;
        this.textComponentService = textComponentService;
    }

    @OmniSubscribe(priority = 99)
    public void onCacheInvalidation(CacheInvalidationRequest request) {

        System.out.println("RECIEVED: " + request);

        if (request.payload() instanceof CacheInvalidationRequest.Profile profilePayload) {

            byte signal = SignalOperations.getSignal(profilePayload.analyticId());

            if (signal == SignalOperations.SIGNAL_PROFILE_ALL) {
                profileEndpoint.clear();
            } else {
                AnalyticsKey analyticsKey = new AnalyticsKey(profilePayload.analyticId());
                UUID uuid = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

                if (uuid == null)
                    return;

                profileEndpoint.invalidate(uuid);
            }



        } else if (request.payload() instanceof CacheInvalidationRequest.Translation transPayload) {

            TranslationEndpoint.DeduplicationKey deduplicationKey = new TranslationEndpoint.DeduplicationKey(
                    transPayload.text(),
                    transPayload.language(),
                    transPayload.translationModule()
            );


            if(SignalOperations.CACHE_TRANSLATION_INVALIDATION_ALL.test(transPayload))
            {
                translationEndpoint.clear();
                textComponentService.clear();
                System.out.println("CLEARED ALL DATA");
            } else {
                System.out.println("NOT  ALL DATA");
                translationEndpoint.invalidate(deduplicationKey);
            }
        }
    }

}
