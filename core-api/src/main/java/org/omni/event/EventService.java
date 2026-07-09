package org.omni.event;

public interface EventService {


    void register(Object listener);

    void unregister(Object listener);

    <T> T callEvent(T event);

}
