package org.pytenix.module.chat;

import org.bukkit.event.Listener;

public class MessageSequencer implements Listener {

    /*
    private final PluginChatModule pluginChatModule;
    private final TextComponentUtil textComponentUtil;

    private final Map<UUID, UserQueue> userQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Cache<IgnoreKey, AtomicInteger> ignoredMessagesCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    public MessageSequencer(PluginChatModule pluginChatModule) {
        this.pluginChatModule = pluginChatModule;
        this.textComponentUtil = pluginChatModule.getTranslatorPlugin().getTextComponentUtil();
        Bukkit.getPluginManager().registerEvents(this, pluginChatModule.getTranslatorPlugin());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    public void translateWithOrder(UUID uuid, Component component, String realMessage, String locale, boolean isOverlay) {
        UserQueue uq = userQueues.computeIfAbsent(uuid, k -> new UserQueue());
        QueuedMessage msg = new QueuedMessage(component, isOverlay);

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
                processQueue(uuid);
            }
        }, 4, TimeUnit.SECONDS);

        textComponentUtil.translateComplexMessage(component, locale, pluginChatModule.getModuleName())
                .whenComplete((translatedComponent, throwable) -> {
                    timeoutTask.cancel(false);

                    if (throwable != null) {
                        System.err.println("[Sequencer] Interner Fehler bei der Übersetzung! Stau wird verhindert.");
                        throwable.printStackTrace();
                        completeMessage(uuid, msg, component);
                    } else {
                        completeMessage(uuid, msg, translatedComponent);
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

    private void completeMessage(UUID uuid, QueuedMessage msg, Component translatedComponent) {
        if (msg.translatedComponent.compareAndSet(null, translatedComponent)) {
            processQueue(uuid);
        }
    }

    private void processQueue(UUID uuid) {
        UserQueue uq = userQueues.get(uuid);
        if (uq == null) return;

        uq.lock.lock();
        try {
            while (!uq.queue.isEmpty()) {
                QueuedMessage head = uq.queue.peek();
                Component compToSend = head.translatedComponent.get();

                if (compToSend != null) {
                    if (sendPacket(uuid, compToSend, head.isOverlay)) {
                        uq.queue.poll();
                    } else {
                        scheduler.schedule(() -> processQueue(uuid), 500, TimeUnit.MILLISECONDS);
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
        Player player = Bukkit.getPlayer(uuid);
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

    private record IgnoreKey(UUID uuid, String json) {}

     */
}