package org.pytenix.module.signs;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.signs.listener.SignViewListener;
import org.pytenix.module.AbstractTranslatorModule;

public class SignsModuleAbstract extends AbstractTranslatorModule {


    //NUR BEIM PLATZIEREN / UPDATEN WIRD ES ÜBERSETZT
    //BEIM ANSCHAUEN GEHT NICHT; MÜSSE MAN MAP_CHUNK AUSEINANDER NEHMEN (LASTIG)

    public SignsModuleAbstract(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "signs");


        PacketEvents.getAPI().getEventManager().registerListener(new SignViewListener(this),
                PacketListenerPriority.NORMAL);

    }


}
