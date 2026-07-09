package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.omni.translation.DefaultTranslatorService;
import org.omni.translation.component.DefaultTextComponentService;
import org.omni.translation.component.TextComponentService;

public class ComponentModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TextComponentService.class).to(DefaultTextComponentService.class).in(Scopes.SINGLETON);
    }
}
