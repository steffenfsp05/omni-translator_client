package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.omni.translation.DefaultTranslatorService;
import org.omni.translation.TranslatorService;

public class TranslationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TranslatorService.class).to(DefaultTranslatorService.class).in(Scopes.SINGLETON);

    }
}
