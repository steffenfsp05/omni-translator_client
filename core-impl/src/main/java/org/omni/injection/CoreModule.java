package org.omni.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.omni.cache.CacheProvider;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.injection.modules.*;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {

        install(new ComponentModule());
        install(new ConfigModule());
        install(new EventModule());
        install(new PacketModule());
        install(new PlaceholderModule());
        install(new TranslationModule());

        bind(new TypeLiteral<CacheProvider<String, String>>() {
        }).to(new TypeLiteral<CaffeineCacheProvider<String, String>>() {
        }).in(Scopes.SINGLETON);
    }
}