package org.pytenix.event;

public interface EventService {


    void register(Object listener);

    void unregister(Object listener);

    <T> T callEvent(T event);

}
