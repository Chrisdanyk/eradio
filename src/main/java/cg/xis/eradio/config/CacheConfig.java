package cg.xis.eradio.config;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.FavoriteRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${recommendations.cache.ttl:3600}") // Default: 1 hour
    private long recommendationsCacheTtl;

    @Autowired(required = false)
    private FavoriteRepository favoriteRepository;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("recommendations");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(recommendationsCacheTtl, TimeUnit.SECONDS)
                        .maximumSize(1000) // Maximum 1000 cached entries
                        .recordStats() // Enable cache statistics
        );
        return cacheManager;
    }

    @Bean("recommendationsKeyGenerator")
    public KeyGenerator recommendationsKeyGenerator() {
        return new KeyGenerator() {
            @Override
            @NonNull
            public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
                if (params.length >= 1 && params[0] instanceof User user) {
                    String favoritesHash = computeFavoritesHash(user);
                    return user.getId() + ":" + favoritesHash;
                }
                // Fallback to default key generation
                StringBuilder sb = new StringBuilder();
                for (Object param : params) {
                    sb.append(param != null ? param.toString() : "null");
                    sb.append(":");
                }
                return sb.toString();
            }

            private String computeFavoritesHash(User user) {
                if (favoriteRepository == null) {
                    return "unknown";
                }
                try {
                    // Use eager fetch query to avoid lazy loading issues
                    List<Favorite> favorites = favoriteRepository.findByUserWithRadioStations(user);

                    if (favorites.isEmpty()) {
                        return "empty";
                    }

                    // Sort UUIDs to ensure consistent hash regardless of order
                    List<String> uuids = favorites.stream()
                            .map(Favorite::getRadioStation)
                            .filter(Objects::nonNull)
                            .map(RadioStation::getStationUuid)
                            .filter(Objects::nonNull)
                            .sorted()
                            .collect(Collectors.toList());

                    if (uuids.isEmpty()) {
                        return "empty";
                    }

                    // Create hash from concatenated UUIDs
                    String combined = String.join(",", uuids);
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] hashBytes = md.digest(combined.getBytes());

                    // Convert to hex string (first 16 chars for shorter key)
                    StringBuilder hexString = new StringBuilder();
                    for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                        String hex = Integer.toHexString(0xff & hashBytes[i]);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    return hexString.toString();
                } catch (NoSuchAlgorithmException e) {
                    // Fallback to simple hash code
                    List<Favorite> favorites = favoriteRepository.findByUserWithRadioStations(user);
                    return String.valueOf(Objects.hash(favorites));
                }
            }
        };
    }
}

