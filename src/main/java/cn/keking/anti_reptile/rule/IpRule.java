package cn.keking.anti_reptile.rule;

import cn.keking.anti_reptile.config.AntiReptileProperties;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public class IpRule extends AbstractRule {

    private final static Logger LOGGER = LoggerFactory.getLogger(IpRule.class);

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AntiReptileProperties properties;

    private static final String RATELIMITER_COUNT_PREFIX = "ratelimiter_request_count";
    private static final String RATELIMITER_EXPIRATIONTIME_PREFIX = "ratelimiter_expirationtime";
    private static final String RATELIMITER_HIT_CRAWLERSTRATEGY = "ratelimiter_hit_crawlerstrategy";

    @Override
    @SuppressWarnings("unchecked")
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        //获取当前请求ip
        String ipAddress = getIpAddr(request);
        //获取ip白名单
        List<String> ignoreIpList = properties.getIpRule().getIgnoreIp();
        //判断是否在ip白名单
        if (ignoreIpList != null && ignoreIpList.size() > 0) {
            for (String ignoreIp : ignoreIpList) {
                if (ignoreIp.endsWith("*")) {
                    ignoreIp = ignoreIp.substring(0, ignoreIp.length() - 1);
                }
                if (ipAddress.startsWith(ignoreIp)) {
                    return false;
                }
            }
        }
        //获取请求url
        String requestUrl = request.getRequestURI();
        //毫秒，默认5000
        int expirationTime = properties.getIpRule().getExpirationTime();
        //最高expirationTime时间内请求数
        int requestMaxSize = properties.getIpRule().getRequestMaxSize();
        //获取请求的数量
        RAtomicLong rRequestCount = redissonClient.getAtomicLong(RATELIMITER_COUNT_PREFIX.concat(requestUrl).concat(ipAddress));
        RAtomicLong rExpirationTime = redissonClient.getAtomicLong(RATELIMITER_EXPIRATIONTIME_PREFIX.concat(requestUrl).concat(ipAddress));
        if (!rExpirationTime.isExists()) {
            rRequestCount.set(0L);
            rExpirationTime.set(0L);
            rExpirationTime.expire(expirationTime, TimeUnit.MILLISECONDS);
        } else {
            RMap rHitMap = redissonClient.getMap(RATELIMITER_HIT_CRAWLERSTRATEGY);
            if ((rRequestCount.incrementAndGet() > requestMaxSize) || rHitMap.containsKey(ipAddress)) {
                //触发爬虫策略 ，默认10天后可重新访问
                long lockExpire = properties.getIpRule().getLockExpire();
                rExpirationTime.expire(lockExpire, TimeUnit.SECONDS);
                //保存触发来源
                rHitMap.put(ipAddress, requestUrl);
                LOGGER.info("Intercepted request, uri: {}, ip：{}, request :{}, times in {} ms。Automatically unlock after {} seconds", requestUrl, ipAddress, requestMaxSize, expirationTime,lockExpire);
                return true;
            }
        }
        return false;
    }

    /**
     * 重置已记录规则
     * @param request 请求
     * @param realRequestUri 原始请求uri
     */
    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        String ipAddress = getIpAddr(request);
        String requestUrl = realRequestUri;
        /**
         * 重置计数器
         */
        int expirationTime = properties.getIpRule().getExpirationTime();//获取时间窗口
        RAtomicLong rRequestCount = redissonClient.getAtomicLong(RATELIMITER_COUNT_PREFIX.concat(requestUrl).concat(ipAddress));
        RAtomicLong rExpirationTime = redissonClient.getAtomicLong(RATELIMITER_EXPIRATIONTIME_PREFIX.concat(requestUrl).concat(ipAddress));
        rRequestCount.set(0L);
        rExpirationTime.set(0L);
        rExpirationTime.expire(expirationTime, TimeUnit.MILLISECONDS);
        /**
         * 清除记录
         */
        RMap rHitMap = redissonClient.getMap(RATELIMITER_HIT_CRAWLERSTRATEGY);
        rHitMap.remove(ipAddress);
    }

    private static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
