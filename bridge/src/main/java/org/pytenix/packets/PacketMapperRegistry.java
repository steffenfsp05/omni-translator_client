package org.pytenix.packets;


import org.pytenix.packets.impl.*;

import java.util.HashMap;
import java.util.Map;

public class PacketMapperRegistry {

    private static final Map<Class<?>, PacketMapper<?, ?>> javaToProto = new HashMap<>();
    private static final Map<Class<?>, PacketMapper<?, ?>> protoToJava = new HashMap<>();


    static {
        register(new ConfigRequestMapper());
        register(new DefaultServerConfigMapper());
        register(new GeoRequestMapper());
        register(new GeoResultMapper());
        register(new TranslationRequestMapper());
        register(new TranslationResultMapper());
    }

    public static <P, J> void register(PacketMapper<P, J> mapper) {
        javaToProto.put(mapper.getJavaClass(), mapper);
        protoToJava.put(mapper.getProtoClass(), mapper);
    }

    @SuppressWarnings("unchecked")
    public static <P, J> P toProto(J javaObject) {
        PacketMapper<P, J> mapper = (PacketMapper<P, J>) javaToProto.get(javaObject.getClass());
        if (mapper == null) throw new IllegalArgumentException("Kein Mapper für " + javaObject.getClass().getSimpleName());
        return mapper.to(javaObject);
    }

    @SuppressWarnings("unchecked")
    public static <P, J> J fromProto(P protoObject) {
        PacketMapper<P, J> mapper = (PacketMapper<P, J>) protoToJava.get(protoObject.getClass());
        if (mapper == null) throw new IllegalArgumentException("Kein Mapper für " + protoObject.getClass().getSimpleName());
        return mapper.from(protoObject);
    }
}