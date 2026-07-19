package org.omni.injection;

import com.google.inject.*;
import org.omni.cache.CacheProvider;
import org.omni.cache.CaffeineCacheProvider;
import org.omni.injection.modules.*;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.secret.CommonAnalyticsSecret;
import org.slf4j.Logger;

import java.nio.file.Path;

public class CoreModule extends AbstractModule {


    private final Path dataDirectory;

    public CoreModule(Path dataDirectory)
    {
        this.dataDirectory = dataDirectory;
    }

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


    @Provides
    @Singleton
    public AbstractAnalyticsSecret provideAnalyticsSecret(Logger logger) {
        return new CommonAnalyticsSecret(logger, dataDirectory);
    }
}