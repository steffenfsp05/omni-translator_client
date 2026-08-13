package org.pytenix.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.pytenix.modules.disconnect.listener.OmniPlayerPreDisconnectListener;

public class InterceptModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<Object> omniListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("omniListeners"));
        omniListeners.addBinding().to(OmniPlayerPreDisconnectListener.class).in(Scopes.SINGLETON);
    }
}
