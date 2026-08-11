package org.omni.injection.modules;

import com.google.inject.*;
import org.omni.event.EventService;
import org.omni.placeholder.*;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.TranslationPipeline;
import org.omni.placeholder.processor.*;

import java.util.Arrays;
import java.util.List;

public class TranslationPipelineModule extends AbstractModule {
    @Override
    protected void configure() {

        bind(TranslationPipeline.class).to(DefaultTranslationPipeline.class).in(Scopes.SINGLETON);

    }

    @Provides
    @Singleton
    public List<TextProcessor> provideTranslationPipeline(
            SystemProtectionProcessor systemProtectionProcessor,
            GradientProcessor gradientProcessor,
            ColorProcessor colorProcessor,
            PriceProcessor priceProcessor,
            NameProtectorProcessor nameProtectorProcessor,
            WordProtectorProcessor wordProtectorProcessor,
            NormalizerProcessor normalizerProcessor
    ) {
        return Arrays.asList(
                systemProtectionProcessor,
                gradientProcessor,
                colorProcessor,
                priceProcessor,
                colorProcessor,
                nameProtectorProcessor,
                wordProtectorProcessor,
                normalizerProcessor
        );
    }
}