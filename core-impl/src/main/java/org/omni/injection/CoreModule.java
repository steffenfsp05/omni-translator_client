package org.omni.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.omni.injection.modules.*;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.impl.*;
import org.omni.packets.registry.DefaultPacketMapperRegistry;

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