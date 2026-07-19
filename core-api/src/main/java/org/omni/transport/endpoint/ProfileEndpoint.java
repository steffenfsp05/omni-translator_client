package org.omni.transport.endpoint;

import org.omni.cache.CacheProvider;
import org.omni.packets.data.ProfileResultData;
import org.omni.transport.EndpointHandler;

import java.util.UUID;

public interface ProfileEndpoint extends EndpointHandler<ProfileResultData, UUID, ProfileResultData>, CacheProvider<UUID, ProfileResultData> {
}
