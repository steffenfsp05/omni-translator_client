package org.pytenix.cache;

public interface CacheProvider<K, V> {


    public void set(K key, V value);

    public V get(K key);

    public void invalidate(K key);

    public boolean exists(K key);


}
