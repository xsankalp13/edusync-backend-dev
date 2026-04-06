package com.project.edusync.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Value("${app.cache.version:v2}")
    private String cacheVersion;

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        String prefix = "edusync:" + cacheVersion + ":";
        log.info("Redis cache namespace version: {} (prefix={})", cacheVersion, prefix);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, "@class");
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(prefix)
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache get failed for cache='{}' key='{}'. Evicting key and continuing without cache. Cause: {}",
                        cache.getName(), key, exception.getMessage());
                try {
                    cache.evict(key);
                } catch (RuntimeException evictException) {
                    log.warn("Cache evict after get failure also failed for cache='{}' key='{}'. Cause: {}",
                            cache.getName(), key, evictException.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache put failed for cache='{}' key='{}'. Continuing without cache write. Cause: {}",
                        cache.getName(), key, exception.getMessage());
            }
        };
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer teacherDashboardSummaryCacheTtlCustomizer() {
        return builder -> builder.withCacheConfiguration(
                "teacherDashboardSummaryV2",
                redisCacheConfiguration().entryTtl(Duration.ofMinutes(5))
        );
    }
}

