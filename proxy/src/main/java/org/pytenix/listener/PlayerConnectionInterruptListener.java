package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.omni.event.EventService;
import org.omni.event.register.player.OmniAsyncPlayerPreDisconnectEvent;
import org.omni.event.register.player.OmniPlayerDisconnectEvent;

@Singleton
public class PlayerConnectionInterruptListener {

    private final EventService eventService;

    @Inject
    public PlayerConnectionInterruptListener(EventService eventService) {
        this.eventService = eventService;
    }

    @Subscribe(order = PostOrder.LATE)
    public EventTask onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return EventTask.withContinuation(continuation -> {

                Component reasonComp = event.getResult().getReasonComponent().orElse(Component.empty());
                String reason = LegacyComponentSerializer.legacyAmpersand().serialize(reasonComp);

                OmniAsyncPlayerPreDisconnectEvent omniEvent = new OmniAsyncPlayerPreDisconnectEvent(
                        event.getPlayer().getUniqueId(),
                        reason
                );

                eventService.callEventAsync(omniEvent).thenAccept(result -> {
                    Component translated = LegacyComponentSerializer.legacyAmpersand().deserialize(result.getReason());
                    event.setResult(LoginEvent.ComponentResult.denied(translated));
                    continuation.resume();
                });
            });
        }
        return null;
    }

    @Subscribe(order = PostOrder.LATE)
    public EventTask onKickedFromServer(KickedFromServerEvent event) {
        return EventTask.withContinuation(continuation -> {

            Component reasonComp = event.getServerKickReason().orElse(Component.empty());
            String reason = LegacyComponentSerializer.legacyAmpersand().serialize(reasonComp);

            OmniAsyncPlayerPreDisconnectEvent omniEvent = new OmniAsyncPlayerPreDisconnectEvent(
                    event.getPlayer().getUniqueId(),
                    reason
            );

            eventService.callEventAsync(omniEvent).thenAccept(result -> {
                Component translated = LegacyComponentSerializer.legacyAmpersand().deserialize(result.getReason());
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(translated));
                continuation.resume();
            });
        });
    }

}
