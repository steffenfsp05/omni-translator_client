package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.omni.config.ConfigService;
import org.omni.config.DefaultConfigService;

public class ConfigModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigService.class).to(DefaultConfigService.class).in(Scopes.SINGLETON);
    }
}
