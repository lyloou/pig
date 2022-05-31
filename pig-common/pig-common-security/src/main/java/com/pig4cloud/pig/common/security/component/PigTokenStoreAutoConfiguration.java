package com.pig4cloud.pig.common.security.component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.pig4cloud.pig.common.security.service.PigRedisOAuth2AuthorizationConsentService;
import com.pig4cloud.pig.common.security.service.PigRedisOAuth2AuthorizationService;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * @author lengleng
 * @date 2021/10/16
 */
public class PigTokenStoreAutoConfiguration {

	// todo redis 模式需要优化
	@Bean
	public OAuth2AuthorizationService authorizationService(RedisTemplate redisTemplate) {
		return new PigRedisOAuth2AuthorizationService(redisTemplate);
	}

	@Bean
	public OAuth2AuthorizationConsentService auth2AuthorizationConsentService(RedisTemplate redisTemplate) {
		return new PigRedisOAuth2AuthorizationConsentService(redisTemplate);
	}

	@Bean
	@SneakyThrows
	public JWKSource<SecurityContext> jwkSource() {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

		// @formatter:off
		RSAKey rsaKey= new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

}
