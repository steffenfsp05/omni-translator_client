package org.pytenix.entity.mapper;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.proto.generated.NetworkPackets;

public interface ServerConfigMapper {


    NetworkPackets.ServerConfiguration to(ServerConfiguration javaConfig);
    ServerConfiguration from(NetworkPackets.ServerConfiguration serverConfiguration);

}
