package com.erencsahin.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        var cfg = new RedisStandaloneConfiguration("localhost", 6379);
        LettuceConnectionFactory cf = new LettuceConnectionFactory(cfg);
        cf.setShareNativeConnection(false);
        return cf;
    }


    @Bean
    @Primary
    public RedisTemplate<String,String> redisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String,String> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new StringRedisSerializer());
        tpl.afterPropertiesSet();
        return tpl;
    }


}
