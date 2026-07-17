package org.pytenix.socket.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.checkerframework.checker.units.qual.A;
import org.omni.entity.TranslationModule;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.translation.component.TextComponentService;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.omni.transport.endpoint.TranslationEndpoint;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
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
    public void onCacheInvalidation(CacheInvalidationRequest request)
    {

        System.out.println("RECIEVED: " + request);

        if (request.payload() instanceof CacheInvalidationRequest.Profile profilePayload) {
            AnalyticsKey analyticsKey = new AnalyticsKey(profilePayload.analyticId());
            UUID uuid = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

            if(uuid == null)
                sendErrorMessage(analyticsKey);

            profileEndpoint.invalidate(uuid);

        } else if (request.payload() instanceof CacheInvalidationRequest.Translation transPayload) {

            TranslationEndpoint.DeduplicationKey deduplicationKey = new TranslationEndpoint.DeduplicationKey(
                    transPayload.text(),
                    transPayload.language(),
                    transPayload.translationModule()
            );

            if (transPayload.text().equals("*") && transPayload.language().equals("*") && transPayload.translationModule().equals(TranslationModule.LIVE_CHAT)) {
                translationEndpoint.clear();
                textComponentService.clear();
                System.out.println("CLEARED ALL DATA");
            } else {
                System.out.println("NOT  ALL DATA");
                translationEndpoint.invalidate(deduplicationKey);
            }
        }
    }


    private void sendErrorMessage(AnalyticsKey analyticId)
    {
        throw new NullPointerException("Analytic ID not reversable from ID " + Base64.getEncoder().encodeToString(analyticId.bytes()));
    }
}
