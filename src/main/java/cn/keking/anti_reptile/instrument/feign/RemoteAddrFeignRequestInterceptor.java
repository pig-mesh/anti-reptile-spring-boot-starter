package cn.keking.anti_reptile.instrument.feign;

import cn.keking.anti_reptile.rule.IpRule;
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
 * 传递用户 remote ip 头
 */
public class RemoteAddrFeignRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate requestTemplate) {

		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
					.getRequest();
			requestTemplate.header("x-forwarded-for", IpRule.getIpAddr(request));
		}
		catch (Exception e) {
			requestTemplate.header(HttpHeaders.USER_AGENT, "feign client");
		}

	}

}
