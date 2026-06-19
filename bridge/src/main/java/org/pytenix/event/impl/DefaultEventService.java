package org.pytenix.event.impl;

import org.pytenix.event.EventService;
import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.handler.RegisteredHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultEventService implements EventService {


    private final Map<Class<?>, RegisteredHandler[]> handlers = new ConcurrentHashMap<>();

    private final ReentrantLock writeLock = new ReentrantLock();

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    /**
     * Registriert alle Methoden eines Objekts, die mit @Subscribe markiert sind.
     */
    public void register(Object listener) {
        writeLock.lock();
        try {
            for (Method method : listener.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(OmniSubscribe.class)) {
                    if (method.getParameterCount() != 1) {
                        System.err.println("Die Methode " + method.getName() + " muss exakt einen Parameter (das Event) haben!");
                        continue;
                    }

                    Class<?> eventType = method.getParameterTypes()[0];
                    OmniSubscribe annotation = method.getAnnotation(OmniSubscribe.class);

                    method.setAccessible(true);
                    MethodHandle handle = lookup.unreflect(method);

                    RegisteredHandler handler = new RegisteredHandler(listener, handle, annotation.priority());
                    addHandler(eventType, handler);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Konnte Listener nicht registrieren!", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Entfernt ein komplettes Objekt aus dem EventManager.
     */
    public void unregister(Object listener) {
        writeLock.lock();
        try {
            for (Map.Entry<Class<?>, RegisteredHandler[]> entry : handlers.entrySet()) {
                Class<?> eventType = entry.getKey();
                RegisteredHandler[] currentHandlers = entry.getValue();

                // Filtere alle Handler raus, die nicht zum übergebenen Objekt gehören
                RegisteredHandler[] newHandlers = Arrays.stream(currentHandlers)
                        .filter(h -> h.getListenerInstance() != listener)
                        .toArray(RegisteredHandler[]::new);

                if (newHandlers.length == 0) {
                    handlers.remove(eventType);
                } else if (newHandlers.length != currentHandlers.length) {
                    handlers.put(eventType, newHandlers);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }


    public <T> T callEvent(T event) {
        RegisteredHandler[] eventHandlers = handlers.get(event.getClass());

        if (eventHandlers != null) {
            for (RegisteredHandler handler : eventHandlers) {
                handler.execute(event);
            }
        }
        return event;
    }

    private void addHandler(Class<?> eventType, RegisteredHandler newHandler) {
        RegisteredHandler[] currentHandlers = handlers.getOrDefault(eventType, new RegisteredHandler[0]);

        RegisteredHandler[] newHandlers = new RegisteredHandler[currentHandlers.length + 1];
        System.arraycopy(currentHandlers, 0, newHandlers, 0, currentHandlers.length);
        newHandlers[currentHandlers.length] = newHandler;

        Arrays.sort(newHandlers);

        handlers.put(eventType, newHandlers);
    }
}
