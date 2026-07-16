package org.omni.transport.endpoint;

import org.omni.packets.data.GeoRequestData;
import org.omni.packets.data.GeoResultData;
import org.omni.transport.EndpointHandler;

public interface GeoEndpoint extends EndpointHandler<GeoResultData, GeoRequestData, String> {
}
