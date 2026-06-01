package org.dep.reimburse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dep.reimburse.entity.ReimburseDoc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ReimburseDoc> reimburseDocRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<ReimburseDoc> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ReimburseDoc.class);

        RedisTemplate<String, ReimburseDoc> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
