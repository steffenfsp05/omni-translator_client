package org.omni.transport.endpoint;

import org.omni.entity.ServerConfiguration;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.transport.EndpointHandler;

public interface ServerConfigurationEndpoint extends EndpointHandler<ServerConfiguration, ConfigurationRequestData, ServerConfiguration> {
}
