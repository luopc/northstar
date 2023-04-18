package org.dromara.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.dromara.northstar.common.model.MailConfigDescription;
import org.dromara.northstar.data.IMailConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

class MailConfigRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IMailConfigRepository repo;
	
	@BeforeEach
	void prepare() {
		factory.setDatabase(15);
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new MailConfigRepoRedisImpl(redisTemplate);
	}
	
	@AfterEach
	void cleanup() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void test() {
		MailConfigDescription mailConfig = MailConfigDescription.builder()
				.emailSMTPHost("smtp.126.com")
				.emailUsername("something@126.com")
				.subscriberList(List.of("someone@qq.com"))
				.build();
		
		repo.save(mailConfig);
		
		assertThat(repo.get()).isEqualTo(mailConfig);
	}

}
