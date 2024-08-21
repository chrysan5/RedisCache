package com.example.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

// 나머지 import 문들

import java.time.Duration;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

//스프링에서 어노테이션으로 선언형 캐싱 기능 구현을 하는 것이다

@Configuration
@EnableCaching //캐싱 어노테이션
public class CacheConfig {

    //RedisCacheManager 구현체를 사용하여 캐싱 기능을 사용할 수 있음
    @Bean
    // CacheManager로 진행해도 정상 동작
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 설정 구성을 먼저 진행한다.
        // Redis를 이용해서 Spring Cache를 사용할 때 Redis 관련 설정을 모아두는 클래스
        RedisCacheConfiguration configuration = RedisCacheConfiguration
                .defaultCacheConfig() //스프링부트 기본 설정 사용
                // null을 캐싱 할것인지 - diable이므로 결과가 null이면 캐싱하지 않겠다는 의미임
                .disableCachingNullValues()
                // 기본 캐시 유지 시간 (Time To Live) - 120초 이후에는 캐시에 있는 데이터가 아니라 새로 조회함(최신화위해)
                .entryTtl(Duration.ofSeconds(120))
                // 캐시를 구분하는 접두사 설정
                .computePrefixWith(CacheKeyPrefix.simple())
                // 캐시에 저장할 값을 어떻게 직렬화 / 역직렬화 할것인지
                .serializeValuesWith(
                        SerializationPair.fromSerializer(RedisSerializer.java())
                );

        //단일 캐싱을 위한 configuration
        RedisCacheConfiguration individual = RedisCacheConfiguration
                .defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(20)) //단일 객체는 찾는 사람이 적으므로 20초만 캐시 저장함
                .enableTimeToIdle() //TTI 적용 -> 20초 이내에 다시 조회될 경우 20초를 다시 첨부터 카운트하는 설정이다.
                .computePrefixWith(CacheKeyPrefix.simple())
                .serializeValuesWith(
                        SerializationPair.fromSerializer(RedisSerializer.json())
                );

        //위의 설정을 사용하는 캐시메니저를 리턴
        return RedisCacheManager
                .builder(redisConnectionFactory)
                //아래 2줄은 individual config가 추가됨으로 추가되는 코드이다.
                .cacheDefaults(configuration)
                .withCacheConfiguration("storeCache", individual) //storeCache라는 이름으로 만들어지는 캐시에 대해서 두번째 config가 동작함
                .cacheDefaults(configuration)
                .build();
    }
}