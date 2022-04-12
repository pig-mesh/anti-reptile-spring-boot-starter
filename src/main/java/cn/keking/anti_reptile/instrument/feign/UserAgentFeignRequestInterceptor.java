package cn.keking.anti_reptile.instrument.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author lengleng
 * @date 2022/4/12
 *
 * 传递用户 UA 头
 */
public class UserAgentFeignRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate requestTemplate) {

		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
					.getRequest();
			requestTemplate.header(HttpHeaders.USER_AGENT, request.getHeader(HttpHeaders.USER_AGENT));
		}
		catch (Exception e) {
			requestTemplate.header(HttpHeaders.USER_AGENT, "feign client");
		}

	}

}
