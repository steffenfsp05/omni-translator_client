package org.omni.transport.endpoint;

import org.omni.packets.data.ProfileExternRequestData;
import org.omni.packets.data.ProfileResultData;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.transport.EndpointHandler;

import java.util.UUID;

public interface ProfileEndpoint extends EndpointHandler<ProfileResultData, UUID, ProfileResultData> {
}
