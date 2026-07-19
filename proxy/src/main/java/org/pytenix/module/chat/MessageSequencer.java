package org.pytenix.module.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.omni.entity.TranslationModule;
import org.omni.translation.TranslatorService;
import org.omni.translation.component.TextComponentService;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class MessageSequencer {

    private final ProxyServer proxyServer;
    private final TextComponentService textComponentService;
    private final TranslatorService translatorService;

    private final Map<UUID, UserQueue> userQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    @Inject
    public MessageSequencer(ProxyServer proxyServer, TextComponentService textComponentService, TranslatorService translatorService) {
        this.proxyServer = proxyServer;
        this.textComponentService = textComponentService;
        this.translatorService = translatorService;
    }

    private boolean sendPacket(UUID uuid, Component comp, boolean isOverlay) {
        Player player = this.proxyServer.getPlayer(uuid).orElse(null);
        if (player == null) return false;


        try {  //TODO: NOT NEEDED IF OMNI_WATERMARK IMPLEMENTED
            comp = translatorService.setMarked(comp);

            if (isOverlay) {
                player.sendActionBar(comp);
            } else {
                player.sendMessage(comp);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }


    public void translateWithOrder(UUID uuid, Component component, String locale, boolean isOverlay) {
        UserQueue uq = userQueues.computeIfAbsent(uuid, k -> new UserQueue());
        QueuedMessage msg = new QueuedMessage(component, isOverlay);

        final long startTime = System.nanoTime();
        uq.lock.lock();
        try {
            uq.queue.add(msg);
        } finally {
            uq.lock.unlock();
        }

        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            if (msg.translatedComponent.compareAndSet(null, component)) {
                System.out.println("[Sequencer] API Hard-Timeout (4s)! Sende Original: " +
                        LegacyComponentSerializer.legacySection().serialize(component));
                processQueue(uuid, startTime);
            }
        }, 4, TimeUnit.SECONDS);

        textComponentService.translateComplexMessage(component, locale, TranslationModule.PLUGIN_CHAT)
                .whenComplete((translatedComponent, throwable) -> {
                    timeoutTask.cancel(false);

                    if (throwable != null) {
                        System.err.println("[Sequencer] Interner Fehler bei der Übersetzung! Stau wird verhindert.");
                        throwable.printStackTrace();
                        completeMessage(startTime, uuid, msg, component);
                    } else {
                        completeMessage(startTime, uuid, msg, translatedComponent);
                    }
                });
    }


    private void completeMessage(final long startTime, UUID uuid, QueuedMessage msg, Component translatedComponent) {
        if (msg.translatedComponent.compareAndSet(null, translatedComponent)) {
            processQueue(uuid, startTime);
        }
    }


    private void processQueue(UUID uuid, long startTime) {
        UserQueue uq = userQueues.get(uuid);
        if (uq == null) return;

        uq.lock.lock();
        try {
            while (!uq.queue.isEmpty()) {
                QueuedMessage head = uq.queue.peek();
                Component compToSend = head.translatedComponent.get();

                if (compToSend != null) {
                    if (sendPacket(uuid, compToSend, head.isOverlay)) {
                        System.out.println("MessageSequencer took " + ((System.nanoTime() - startTime) / 1000000) + " ms for " + compToSend.toString().substring(0, Math.min(compToSend.toString().length(), 15)));
                        uq.queue.poll();
                    } else {
                        scheduler.schedule(() -> processQueue(uuid, startTime), 500, TimeUnit.MILLISECONDS);
                        break;
                    }
                } else {
                    break;
                }
            }
        } finally {
            uq.lock.unlock();
        }
    }


    public void cleanup(UUID uuid) {
        userQueues.remove(uuid);
    }

    private static class UserQueue {
        final Queue<QueuedMessage> queue = new ArrayDeque<>();
        final ReentrantLock lock = new ReentrantLock();
    }

    private static class QueuedMessage {
        final Component originalComponent;
        final boolean isOverlay;
        final AtomicReference<Component> translatedComponent = new AtomicReference<>(null);

        QueuedMessage(Component originalComponent, boolean isOverlay) {
            this.originalComponent = originalComponent;
            this.isOverlay = isOverlay;
        }
    }

    private record IgnoreKey(UUID uuid, String json) {
    }
}