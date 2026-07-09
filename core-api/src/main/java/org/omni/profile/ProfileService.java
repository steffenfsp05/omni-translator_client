package org.omni.profile;

import com.github.benmanes.caffeine.cache.Cache;
import org.omni.packets.data.ProfileResultData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ProfileService {

    public abstract Cache<UUID, ProfileResultData> cacheProvider();

    public abstract CompletableFuture<ProfileResultData> retrieveProfile(UUID uuid);

    public abstract void updateProfile(ProfileResultData profileData);

    public abstract void handleProfileResult(ProfileResultData resultData);

}
