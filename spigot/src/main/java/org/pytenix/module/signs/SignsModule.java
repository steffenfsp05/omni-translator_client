package org.pytenix.module.signs;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.pytenix.module.signs.listener.SignViewListener;
import org.pytenix.translation.AbstractTranslatorModule;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.locale.PlayerLocaleProcessor;

public class SignsModule extends AbstractTranslatorModule {


    //NUR BEIM PLATZIEREN / UPDATEN WIRD ES ÜBERSETZT
    //BEIM ANSCHAUEN GEHT NICHT; MÜSSE MAN MAP_CHUNK AUSEINANDER NEHMEN (LASTIG)

    public SignsModule(TranslatorService translatorService, PlayerLocaleProcessor playerLocaleProcessor) {
        super(translatorService, "signs", playerLocaleProcessor);


        PacketEvents.getAPI().getEventManager().registerListener(new SignViewListener(this),
                PacketListenerPriority.NORMAL);

    }


}
