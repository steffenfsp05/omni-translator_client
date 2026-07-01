package org.pytenix.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.gson.Gson;
import org.pytenix.cache.CacheProvider;
import org.pytenix.packets.impl.ProfileMapper;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProfileService {


    public final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> queue = new ConcurrentHashMap<>();

    public abstract Cache<UUID, ProfileMapper.ProfileData> cacheProvider();


    public abstract CompletableFuture<ProfileMapper.ProfileData> retrieveProfile(UUID uuid);
    public abstract void updateProfile(ProfileMapper.ProfileData profileData);



    public abstract void handleProfileResult(ProfileMapper.ProfileData resultData);


}
