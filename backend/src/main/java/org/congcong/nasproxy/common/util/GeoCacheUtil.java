package org.congcong.nasproxy.common.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.Pair;
import org.congcong.nasproxy.core.monitor.connect.IPGeoUtil;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GeoCacheUtil {


    private static final Cache<String, Pair<String, String>> ipGeoCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(120, TimeUnit.SECONDS)
            .build();


    /**
     *
     * @param host
     * @return 返回地理位置与ip地址
     */
    public static Pair<String, String> getCountryAndIpAddress(String host) {
        Pair<String, String> ifPresent = ipGeoCache.getIfPresent(host);
        if (ifPresent == null) {
            Optional<IPGeoUtil.LocationInfo> location = IPGeoUtil.getInstance().getLocation(host);
            if (location.isPresent()) {
                IPGeoUtil.LocationInfo locationInfo = location.get();
                String country = locationInfo.getCountry();
                String ip = locationInfo.getIp();
                if (country != null) {
                    ifPresent = new Pair<>(country.toLowerCase(), ip);
                    ipGeoCache.put(host, ifPresent);
                } else {
                    ifPresent = new Pair<>("china", host);
                }
            } else {
                // default
                ifPresent = new Pair<>("china", host);
            }
        }
        return ifPresent;
    }


}
