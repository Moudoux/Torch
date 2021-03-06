package org.torch.server;

import com.destroystokyo.paper.PaperConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;

import lombok.Getter;
import net.minecraft.server.MCUtil;
import net.minecraft.server.UserCache;
import net.minecraft.server.EntityHuman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.spigotmc.SpigotConfig;
import org.torch.api.Async;
import org.torch.api.TorchReactor;

import static net.minecraft.server.UserCache.isOnlineMode;
import static org.torch.server.TorchServer.logger;

@Getter @ThreadSafe
public final class TorchUserCache implements TorchReactor {
    /** The legacy */
    private final UserCache servant;
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    
    // Used to reduce date create
    private final static long DATE_WARP_INTERVAL = TimeUnit.MILLISECONDS.convert(9, TimeUnit.MINUTES);
    private static volatile long lastWarpExpireDate;
    private static volatile Date lastExpireDate;
    
    /**
     * All user caches, Username -> Entry(profile and expire date included)
     */
    private final Cache<String, UserCacheEntry> caches = Caffeine.newBuilder().maximumSize(SpigotConfig.userCacheCap).build();
    
    /** GameProfile repository */
    private final GameProfileRepository profileRepo;
    /** Gson */
    protected final Gson gson;
    /**
     * UserCache file
     * */
    private final File usercacheFile;
    
    public static final ParameterizedType type = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { UserCacheEntry.class};
        }
        
        @Override
        public Type getRawType() {
            return List.class;
        }
        
        @Override
        public Type getOwnerType() {
            return null;
        }
    };
    
    public static boolean authUUID() {
        return isOnlineMode() || (SpigotConfig.bungee && PaperConfig.bungeeOnlineMode);
    }

    public TorchUserCache(GameProfileRepository repo, File file, UserCache legacy) {
        lastExpireDate = warpExpireDate(true);
        
        servant = legacy;
        profileRepo = repo;
        usercacheFile = file;
        
        this.gson = new GsonBuilder().registerTypeHierarchyAdapter(UserCacheEntry.class, new CacheSerializer()).create();
        
        this.load();
    }

    /**
     * Lookup the profile by the name from Mojang, triggering network operation
     */
    @Nullable
    public static GameProfile matchProfile(GameProfileRepository profileRepo, String keyUsername) {
        // Keep current case for offline servers
        if (!authUUID()) {
            return new GameProfile(EntityHuman.offlinePlayerUUID(keyUsername, false), keyUsername);
        }
        
        final GameProfile[] profile = new GameProfile[1];
        ProfileLookupCallback callback = new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile gameprofile) {
                profile[0] = gameprofile;
            }
            
            @Override
            public void onProfileLookupFailed(GameProfile gameprofile, Exception ex) {
                logger.warn("Failed to lookup player {}, using local UUID.", gameprofile.getName());
                profile[0] = new GameProfile(EntityHuman.offlinePlayerUUID(keyUsername), keyUsername);
            }
        };
        
        profileRepo.findProfilesByNames(new String[] { keyUsername }, Agent.MINECRAFT, callback);
        
        return profile[0];
    }
    
    /**
     * Generate an new expire date for the cache
     * */
    public static Date warpExpireDate(boolean force) {
        long now = System.currentTimeMillis();
        if (force || (now - lastWarpExpireDate) > DATE_WARP_INTERVAL) {
            lastWarpExpireDate = now;
            Calendar calendar = Calendar.getInstance();
            
            calendar.setTimeInMillis(now);
            calendar.add(Calendar.MONTH, 1); // TODO: configurable expire date
            return lastExpireDate = calendar.getTime();
        }
        
        return lastExpireDate;
    }
    
    public UserCacheEntry refreshExpireDate(UserCacheEntry entry) {
        return new UserCacheEntry(entry.profile, warpExpireDate(false));
    }
    
    public boolean isExpired(UserCacheEntry entry) {
        return System.currentTimeMillis() >= entry.expireDate.getTime();
    }

    /**
     * Add an entry to this cache with the default expire date
     */
    public UserCacheEntry putCache(String keyUsername) {
        return putCache(keyUsername, (Date) null);
    }

    /**
     * Add an entry to this cache with an expire date, return the new entry
     */
    public UserCacheEntry putCache(String keyUsername, Date date) {
        if (date == null) date = warpExpireDate(false);
        
        UserCacheEntry entry = new UserCacheEntry(matchProfile(profileRepo, keyUsername), date);
        caches.put(keyUsername, entry);
        
        if(!org.spigotmc.SpigotConfig.saveUserCacheOnStopOnly) this.save();
        
        return entry;
    }
    
    /**
     * Also create new entry if not present
     */
    @Nullable
    public GameProfile requestProfile(String username) {
        if (StringUtils.isBlank(username)) return null;
        
        String keyUsername = authUUID() ? username : Caches.toLowerCase(username, Locale.ROOT);
        UserCacheEntry cachedEntry = caches.getIfPresent(keyUsername);
        
        // Remove expired entry
        if (cachedEntry != null) {
            if (isExpired(cachedEntry)) {
                caches.invalidate(keyUsername);
                return null;
            }
        } else {
            cachedEntry = putCache(keyUsername);
        }
        
        return cachedEntry == null ? null : cachedEntry.profile;
    }
    
    @Nullable
    public GameProfile peekCachedProfile(String username) {
        UserCacheEntry entry = caches.getIfPresent(username);
        
        return entry == null ? null : entry.profile;
    }
    
    /** Offer or replace the old cache if present */
    public void offerCache(GameProfile profile) {
        offerCache(profile, warpExpireDate(false));
    }
    
    /** Offer or replace the old cache if present, with an expire date */
    public void offerCache(GameProfile profile, Date date) {
        String keyUsername = authUUID() ? profile.getName() : Caches.toLowerCase(profile.getName(), Locale.ROOT);
        UserCacheEntry entry = caches.getIfPresent(keyUsername);
        
        if (entry != null) {
            
            // The offered profile may has an incorrect case, this only happened on offline servers,
            // replace with an lower-case profile.
            if (!isOnlineMode() && !entry.profile.getName().equals(profile.getName())) {
                entry = new UserCacheEntry(matchProfile(profileRepo, keyUsername), date);
            } else {
                entry = refreshExpireDate(entry);
            }
        } else {
            entry = new UserCacheEntry(profile, date);
        }
        
        caches.put(keyUsername, entry);
        
        if(!SpigotConfig.saveUserCacheOnStopOnly) this.save();
    }
    
    /** Offer an entry, called on load caches */
    private void offerCache(UserCacheEntry entry) {
        Validate.notNull(entry);
        if (isExpired(entry)) return;
        
        caches.put(authUUID() ? entry.profile.getName() : entry.profile.getName().toLowerCase(Locale.ROOT), entry);
    }
    
    public String[] getCachedUsernames() {
        return caches.asMap().keySet().toArray(new String[caches.asMap().size()]);
    }
    
    public void load() {
        BufferedReader reader = null;

        try {
            reader = Files.newReader(usercacheFile, Charsets.UTF_8);
            List<UserCacheEntry> entries = this.gson.fromJson(reader, type);
            
            caches.invalidateAll();
            
            if (entries != null) {
                for (UserCacheEntry entry : Lists.reverse(entries)) {
                    if (entry != null) this.offerCache(entry);
                }
                
                if(!SpigotConfig.saveUserCacheOnStopOnly) this.save();
            }
            
        } catch (FileNotFoundException e) {
            ;
        } catch (JsonSyntaxException e) {
            logger.warn("Usercache.json is corrupted or has bad formatting. Deleting it to prevent further issues.");
            this.usercacheFile.delete();
        } catch (JsonParseException e) {
            ;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
    
    @Async
    public void save() {
        save(true);
    }
    
    public void save(boolean async) {
        Runnable save = () -> {
            String jsonString = this.gson.toJson(this.collectEntries());
            BufferedWriter writer = null;
            
            try {
                writer = Files.newWriter(this.usercacheFile, Charsets.UTF_8);
                writer.write(jsonString);
                return;
            } catch (FileNotFoundException e) {
                return;
            } catch (IOException io) {
                ;
            } finally {
                IOUtils.closeQuietly(writer);
            }
        };
        
        if (async) {
            MCUtil.scheduleAsyncTask(save);
        } else {
            save.run();
        }
    }
    
    /**
     * Returns a list contains all cached entries, size limited with {@value SpigotConfig#userCacheCap}
     */
    public ArrayList<UserCacheEntry> collectEntries() {
        return Lists.newArrayList(caches.asMap().values());
    }
    
    @Getter
    public final class UserCacheEntry {
        /** The player's GameProfile */
        private final GameProfile profile;
        /** The date that this entry will expire */
        private final Date expireDate;
        
        private UserCacheEntry(GameProfile gameProfile, Date date) {
            this.profile = gameProfile;
            this.expireDate = date;
        }
        
        @Deprecated
        public UserCache.UserCacheEntry toLegacy() {
            return servant.new UserCacheEntry(profile, expireDate);
        }
    }
    
    private final class CacheSerializer implements JsonDeserializer<UserCacheEntry>, JsonSerializer<UserCacheEntry> {
        private CacheSerializer() {}

        @Override
        public JsonElement serialize(UserCacheEntry entry, Type type, JsonSerializationContext context) {
            JsonObject jsonData = new JsonObject();
            UUID uuid = entry.profile.getId();

            jsonData.addProperty("name", entry.profile.getName());
            jsonData.addProperty("uuid", uuid == null ? "" : uuid.toString());
            jsonData.addProperty("expiresOn", DATE_FORMAT.format(entry.expireDate));
            
            return jsonData;
        }
        
        @Override
        public UserCacheEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (!jsonElement.isJsonObject()) return null;
            
            JsonObject jsonData = jsonElement.getAsJsonObject();
            JsonElement name = jsonData.get("name");
            JsonElement uuid = jsonData.get("uuid");
            JsonElement expireDate = jsonData.get("expiresOn");
            
            if (name == null || uuid == null) return null;
            
            String uuidString = uuid.getAsString();
            String nameString = name.getAsString();
            
            Date date = null;
            if (expireDate != null) {
                try {
                    date = DATE_FORMAT.parse(expireDate.getAsString());
                } catch (ParseException ex) {
                    ;
                }
            }
            
            if (nameString == null || uuidString == null) return null;
            
            UUID standardUUID;
            try {
                standardUUID = UUID.fromString(uuidString);
            } catch (Throwable t) {
                return null;
            }
            
            return new UserCacheEntry(new GameProfile(standardUUID, nameString), date);
        }
    }
}
