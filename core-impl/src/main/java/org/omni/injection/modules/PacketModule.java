package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Singleton;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.impl.*;
import org.omni.packets.registry.DefaultPacketMapperRegistry;

public class PacketModule extends AbstractModule {

    @Override
    protected void configure() {

        @SuppressWarnings("rawtypes")
        Multibinder<AbstractPacketMapper> mapperBinder =
                Multibinder.newSetBinder(binder(), AbstractPacketMapper.class);

        mapperBinder.addBinding().to(ConfigRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(DefaultServerConfigMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(GeoRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(GeoResultMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(TranslationRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(TranslationResultMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(ProfileMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(ConsentRefreshRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(HeartBeatRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(TrackPlayerRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(InternProfileRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(ExternProfileRequestMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(ExternProfileUpdateMapper.class).in(Scopes.SINGLETON);
        mapperBinder.addBinding().to(CacheInvalidationRequestMapper.class).in(Scopes.SINGLETON);

        bind(PacketMapperRegistry.class).to(DefaultPacketMapperRegistry.class).in(Scopes.SINGLETON);

    }
}