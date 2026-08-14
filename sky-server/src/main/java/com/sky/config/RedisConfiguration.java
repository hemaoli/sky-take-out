package com.sky.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sky.utils.RedisTtlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * Redis 公共Json序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        objectMapper.registerModule(new JavaTimeModule());
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建Redis模板对象");
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        return redisTemplate;
    }

    /**
     * spring cache缓存管理器
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()))
                //兜底过期时间,实际写入时会被随机值覆盖
                .entryTtl(Duration.ofHours(1));

        //默认写入器,作为内部委托
        RedisCacheWriter delegate = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        //自定义缓存写入器:每次写入时生成随机TTL(1~24小时),避免缓存同时过期造成雪崩
        RedisCacheWriter cacheWriter = new RedisCacheWriter() {
            @Override
            public void put(String name, byte[] key, byte[] value, Duration ttl) {
                Duration randomTtl = Duration.ofMinutes(RedisTtlUtil.getRandomMinute());
                delegate.put(name, key, value, randomTtl);
            }

            @Override
            public byte[] get(String name, byte[] key) {
                return delegate.get(name, key);
            }

            @Override
            public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
                return delegate.putIfAbsent(name, key, value, ttl);
            }

            @Override
            public void remove(String name, byte[] key) {
                delegate.remove(name, key);
            }

            @Override
            public void clean(String name, byte[] pattern) {
                delegate.clean(name, pattern);
            }

            @Override
            public void clearStatistics(String name) {
                delegate.clearStatistics(name);
            }

            @Override
            public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector collector) {
                return delegate.withStatisticsCollector(collector);
            }

            @Override
            public CacheStatistics getCacheStatistics(String name) {
                return delegate.getCacheStatistics(name);
            }
        };

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}
