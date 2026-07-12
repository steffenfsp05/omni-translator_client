package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.omni.event.EventService;
import org.omni.event.impl.DefaultEventService;

public class EventModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventService.class).to(DefaultEventService.class).in(Scopes.SINGLETON);
    }
}