package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.omni.event.EventService;
import org.omni.event.impl.DefaultEventService;
import org.omni.placeholder.listener.ConfigUpdateListener;
import org.omni.placeholder.listener.PlayerConnectListener;
import org.omni.placeholder.listener.PlayerDisconnectListener;

public class EventModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventService.class).to(DefaultEventService.class).in(Scopes.SINGLETON);

        Multibinder<Object> omniListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("omniListeners"));
        omniListeners.addBinding().to(ConfigUpdateListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(PlayerConnectListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(PlayerDisconnectListener.class).in(Scopes.SINGLETON);
    }
}