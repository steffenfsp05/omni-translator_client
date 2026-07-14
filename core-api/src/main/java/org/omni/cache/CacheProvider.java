package org.omni.cache;

public interface CacheProvider<K, V> {


    void set(K key, V value);

    V get(K key);

    void invalidate(K key);

    boolean exists(K key);


}
