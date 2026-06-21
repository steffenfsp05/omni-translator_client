package org.pytenix.packets;

import lombok.Getter;

@Getter
public abstract class AbstractPacketMapper<P,J> {
    private final Class<P> protoClass;
    private final Class<J> javaClass;

    public AbstractPacketMapper(Class<P> protoClass, Class<J> javaClass) {
        this.protoClass = protoClass;
        this.javaClass = javaClass;
    }

    public abstract P to(J packet);
    public abstract J from(P packet);


}
