package org.pytenix.chat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.util.TextComponentUtil;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class MessageSequencer {

    private final TranslatorPlugin translatorPlugin;
    private final TextComponentUtil textComponentUtil;

    private final Map<UUID, UserQueue> userQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Cache<IgnoreKey, AtomicInteger> ignoredMessagesCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    public MessageSequencer(
            TranslatorPlugin translatorPlugin,
            TextComponentUtil textComponentUtil
    ) {
        this.translatorPlugin = translatorPlugin;
        this.textComponentUtil = textComponentUtil;
    }


    public void translateWithOrder(UUID uuid, Component component, String realMessage, String locale, boolean isOverlay) {
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
                processQueue(uuid,startTime);
            }
        }, 4, TimeUnit.SECONDS);

        textComponentUtil.translateComplexMessage(component, locale, ServerConfiguration.Module.PLUGIN_CHAT.getModuleName())
                .whenComplete((translatedComponent, throwable) -> {
                    timeoutTask.cancel(false);

                    if (throwable != null) {
                        System.err.println("[Sequencer] Interner Fehler bei der Übersetzung! Stau wird verhindert.");
                        throwable.printStackTrace();
                        completeMessage(startTime,uuid, msg, component);
                    } else {
                        completeMessage(startTime,uuid, msg, translatedComponent);
                    }
                });
    }

    public void ignoreNextMessage(UUID uuid, Component component) {
        try {
            String json = GsonComponentSerializer.gson().serialize(component);
            IgnoreKey key = new IgnoreKey(uuid, json);

            ignoredMessagesCache.get(key, k -> new AtomicInteger(0)).incrementAndGet();
        } catch (Exception ignored) {
        }
    }

    public boolean isIgnored(UUID uuid, Component component) {
        try {
            String json = GsonComponentSerializer.gson().serialize(component);
            IgnoreKey key = new IgnoreKey(uuid, json);

            AtomicInteger count = ignoredMessagesCache.getIfPresent(key);
            if (count != null && count.get() > 0) {
                if (count.decrementAndGet() <= 0) {
                    ignoredMessagesCache.invalidate(key);
                }
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void completeMessage(final long startTime, UUID uuid, QueuedMessage msg, Component translatedComponent) {
        if (msg.translatedComponent.compareAndSet(null, translatedComponent)) {
            processQueue(uuid,startTime);
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
                        scheduler.schedule(() -> processQueue(uuid,startTime), 500, TimeUnit.MILLISECONDS);
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

    private boolean sendPacket(UUID uuid, Component comp, boolean isOverlay) {
        Player player = this.translatorPlugin.getProxyServer().getPlayer(uuid).orElse(null);
        if (player == null) return false;

        try {
            ignoreNextMessage(uuid, comp);

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