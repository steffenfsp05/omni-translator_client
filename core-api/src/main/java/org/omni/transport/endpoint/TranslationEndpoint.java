package org.omni.transport.endpoint;

import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.transport.EndpointHandler;

public interface TranslationEndpoint extends EndpointHandler<TranslationResultData, TranslationRequestData, String> {
}
