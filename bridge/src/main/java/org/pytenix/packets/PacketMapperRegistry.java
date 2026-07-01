package org.pytenix.packets;


import org.pytenix.packets.impl.*;

import java.util.HashMap;
import java.util.Map;

public class PacketMapperRegistry {

    private static final Map<Class<?>, AbstractPacketMapper<?, ?>> javaToProto = new HashMap<>();
    private static final Map<Class<?>, AbstractPacketMapper<?, ?>> protoToJava = new HashMap<>();


    static {
        register(new ConfigRequestMapper());
        register(new DefaultServerConfigMapper());
        register(new GeoRequestMapper());
        register(new GeoResultMapper());
        register(new TranslationRequestMapper());
        register(new TranslationResultMapper());
        register(new ProfileMapper());
        register(new ConsentRefreshRequestMapper());
        register(new HeartBeatRequestMapper());
        register(new TrackPlayerRequestMapper());
    }

    public static <P, J> void register(AbstractPacketMapper<P, J> mapper) {
        javaToProto.put(mapper.getJavaClass(), mapper);
        protoToJava.put(mapper.getProtoClass(), mapper);
    }

    @SuppressWarnings("unchecked")
    public static <P, J> P toProto(J javaObject) {
        AbstractPacketMapper<P, J> mapper = (AbstractPacketMapper<P, J>) javaToProto.get(javaObject.getClass());
        if (mapper == null)
            throw new IllegalArgumentException("Kein Mapper für " + javaObject.getClass().getSimpleName());
        return mapper.to(javaObject);
    }

    @SuppressWarnings("unchecked")
    public static <P, J> J fromProto(P protoObject) {
        AbstractPacketMapper<P, J> mapper = (AbstractPacketMapper<P, J>) protoToJava.get(protoObject.getClass());
        if (mapper == null)
            throw new IllegalArgumentException("Kein Mapper für " + protoObject.getClass().getSimpleName());
        return mapper.from(protoObject);
    }
}