package org.omni.packets.registry;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.PacketMapperRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class DefaultPacketMapperRegistry implements PacketMapperRegistry {

    private final Map<Class<?>, AbstractPacketMapper<?, ?>> javaToProto = new HashMap<>();
    private final Map<Class<?>, AbstractPacketMapper<?, ?>> protoToJava = new HashMap<>();

    @Inject
    @SuppressWarnings("rawtypes")
    public DefaultPacketMapperRegistry(Set<AbstractPacketMapper> mappers) {

        for (AbstractPacketMapper<?, ?> mapper : mappers) {
            javaToProto.put(mapper.getJavaClass(), mapper);
            protoToJava.put(mapper.getProtoClass(), mapper);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P, J> P toProto(J javaObject) {
        AbstractPacketMapper<P, J> mapper = (AbstractPacketMapper<P, J>) javaToProto.get(javaObject.getClass());
        if (mapper == null)
            throw new IllegalArgumentException("Kein Mapper für " + javaObject.getClass().getSimpleName() + " in Method toProto.");
        return mapper.to(javaObject);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P, J> J fromProto(P protoObject) {
        AbstractPacketMapper<P, J> mapper = (AbstractPacketMapper<P, J>) protoToJava.get(protoObject.getClass());
        if (mapper == null)
            throw new IllegalArgumentException("Kein Mapper für " + protoObject.getClass().getSimpleName() + " in Method fromProto");
        return mapper.from(protoObject);
    }
}