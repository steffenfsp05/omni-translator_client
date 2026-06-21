package org.pytenix.packets;

import lombok.Getter;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.proto.generated.NetworkPackets;

@Getter
public abstract class PacketMapper<P,J> {
    private final Class<P> protoClass;
    private final Class<J> javaClass;

    public PacketMapper(Class<P> protoClass, Class<J> javaClass) {
        this.protoClass = protoClass;
        this.javaClass = javaClass;
    }

    public abstract P to(J packet);
    public abstract J from(P packet);


}
