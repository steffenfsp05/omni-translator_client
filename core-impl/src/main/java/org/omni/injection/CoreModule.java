package org.omni.injection;

import com.google.inject.AbstractModule;
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
    }
}