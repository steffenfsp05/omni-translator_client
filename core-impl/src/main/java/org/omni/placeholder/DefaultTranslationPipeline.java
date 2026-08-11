package org.omni.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.EventService; // Angenommener Name deines Event-Managers
import org.omni.event.impl.DefaultEventService;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.TranslationPipeline;
import org.omni.placeholder.processor.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Singleton
public class DefaultTranslationPipeline implements TranslationPipeline {

    private final List<TextProcessor> processors;

    public static void main(String[] args) {

        List<TextProcessor> a = new ArrayList<>();

        a.add(new SystemProtectionProcessor());
        a.add(new GradientProcessor());
        a.add(new ColorProcessor());
        a.add(new PriceProcessor());
        a.add(new NameProtectorProcessor());
        a.add(new WordProtectorProcessor());
        a.add(new NormalizerProcessor());

        new DefaultTranslationPipeline(new DefaultEventService(), a);
    }



    @Inject
    public DefaultTranslationPipeline(
            EventService eventService,
            List<TextProcessor> list
    ) {

        this.processors = list;
        for (TextProcessor processor : processors) {
            eventService.register(processor);
        }

        String itemLore = "§eLevel 1\n" +
                "§7Level 1\n" +
                "§7Rewards:\n" +
                "§9  +1 ✿ Wisdom\n" +
                "§c  +0.4 ❤ Health\n" +
                "\n" +
                "§6Alchemist §aAbility Unlock\n" +
                "§7  Potions you brew have a 3% longer duration.\n" +
                "\n" +
                "§7Progress: §e0%\n" +
                "§e■§7■■■■■■■■■■■■■■■■■■■\n" +
                "§70/100 XP\n" +
                "\n" +
                "§eIN PROGRESS";
      testText(itemLore);





    }



    private void testText(String text)
    {
        final UUID uuid = UUID.randomUUID();
        System.out.println("TEXT: " + text);
        text = prepare(uuid, text);
        System.out.println("prepare " + text);
        text = restore(uuid, text);
        System.out.println("restore " + text);
    }

    @Override
    public String prepare(UUID id, String text) {
        String current = text;
        for (TextProcessor p : processors) {
            current = p.process(id, current);
        }
        return current;
    }

    @Override
    public String restore(UUID id, String text) {
        String current = text;
        List<TextProcessor> reversed = new ArrayList<>(processors);
        Collections.reverse(reversed);

        for (TextProcessor p : reversed) {
            current = p.restore(id, current);
        }
        return current;
    }
}