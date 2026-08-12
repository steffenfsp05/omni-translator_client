package org.pytenix.module.gui.cache;

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

import java.util.UUID;

@Singleton
public class CacheInventoryInvalidationListener {


    final ItemStackCache itemStackCache;

    @Inject
    public CacheInventoryInvalidationListener(ItemStackCache itemStackCache) {
        this.itemStackCache = itemStackCache;
    }

    @OmniSubscribe(priority = 50)
    public void onCacheInvalidation(CacheInvalidationRequest request) {

        System.out.println("onCacheInvalidation RECIEVED: " + request);
        if (request.payload() instanceof CacheInvalidationRequest.Translation transPayload) {

            if(SignalOperations.CACHE_TRANSLATION_INVALIDATION_ALL.test(transPayload))
            {
                if(transPayload.translationModule() == TranslationModule.GUI)
                    itemStackCache.clearCache();
            }
        }
    }

}
