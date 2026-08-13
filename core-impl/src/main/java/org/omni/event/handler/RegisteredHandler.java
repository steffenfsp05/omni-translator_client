package org.omni.event.handler;

import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.CompletableFuture;

public class RegisteredHandler implements Comparable<RegisteredHandler> {
    @Getter
    private final Object listenerInstance;
    private final MethodHandle methodHandle;
    private final int priority;

    public RegisteredHandler(Object listenerInstance, MethodHandle methodHandle, int priority) {
        this.listenerInstance = listenerInstance;
        this.methodHandle = methodHandle;
        this.priority = priority;
    }

    public CompletableFuture<Void> executeAsync(Object event) {
        try {
            Object result = methodHandle.invoke(listenerInstance, event);

            if (result instanceof CompletableFuture<?>) {
                return ((CompletableFuture<?>) result).thenAccept(v -> {});
            }
        } catch (Throwable t) {
            System.err.println("Fehler beim Ausführen des Events im Listener: " + listenerInstance.getClass().getName());
            t.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    public void execute(Object event) {
        try {
            methodHandle.invoke(listenerInstance, event);
        } catch (Throwable t) {
            System.err.println("Fehler beim Ausführen des Events im Listener: " + listenerInstance.getClass().getName());
            t.printStackTrace();
        }
    }

    @Override
    public int compareTo(RegisteredHandler other) {
        return Integer.compare(this.priority, other.priority);
    }
}