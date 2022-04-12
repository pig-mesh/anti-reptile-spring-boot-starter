package cn.keking.anti_reptile.instrument.feign;

import feign.Client;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author purgeyao
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Client.class, FeignContext.class })
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignClientAutoConfiguration {

	@Bean
	public UserAgentFeignRequestInterceptor userAgentFeignRequestInterceptor() {
		return new UserAgentFeignRequestInterceptor();
	}

	@Bean
	public RemoteAddrFeignRequestInterceptor remoteAddrFeignRequestInterceptor() {
		return new RemoteAddrFeignRequestInterceptor();
	}

}
