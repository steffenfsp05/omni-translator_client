package org.omni.transport.endpoint;

import org.omni.cache.CacheProvider;
import org.omni.entity.TranslationModule;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.transport.EndpointHandler;

public interface TranslationEndpoint extends EndpointHandler<TranslationResultData, TranslationRequestData, String>, CacheProvider<TranslationEndpoint.DeduplicationKey, String> {

    public record DeduplicationKey(String text, String lang, TranslationModule translationModule) {
    }
}
