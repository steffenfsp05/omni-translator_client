package org.pytenix.roi.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.pytenix.roi.listener.PlayerConnectListener;
import org.pytenix.roi.listener.PlayerDisconnectListener;
import org.pytenix.roi.sender.HeartBeatSender;
import org.pytenix.roi.service.PlayerTrackerService;

@Singleton
public class RoiModule extends AbstractModule {


    @Override
    protected void configure() {

        bind(HeartBeatSender.class).asEagerSingleton();


        Multibinder<Object> omniListeners = Multibinder.newSetBinder(binder(), Object.class, Names.named("omniListeners"));
        omniListeners.addBinding().to(PlayerConnectListener.class).in(Scopes.SINGLETON);
        omniListeners.addBinding().to(PlayerDisconnectListener.class).in(Scopes.SINGLETON);

    }


}
