package org.congcong.nasproxy.core.monitor.connect;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

@Slf4j
public class IPGeoUtil {
    private static DatabaseReader reader;

    // 单例模式初始化
    private static class IPGeoUtilHolder {
        private static final IPGeoUtil INSTANCE = new IPGeoUtil();
    }

    private IPGeoUtil() {
        try {
            String jarDir = System.getProperty("user.dir");  // 获取当前工作目录（JAR所在的目录）
            File workDir = new File(jarDir);
            File[] files = workDir.listFiles();
            if (files != null) {
                File mmdb = null;
                for (File file : files) {
                    if (file.getName().contains("mmdb")) {
                        mmdb = file;
                        break;
                    }
                }
                if (mmdb != null) {
                    log.info("load GeoLite2-City.mmdb");
                    reader = new DatabaseReader.Builder(mmdb).build();
                } else {
                    log.info("not find mmdb");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("无法加载GeoIP数据库", e);
        }
    }

    public static IPGeoUtil getInstance() {
        return IPGeoUtilHolder.INSTANCE;
    }

    /**
     * 获取IP的详细地理位置信息
     * @param ip IP地址
     * @return 地理位置信息
     */
    public Optional<LocationInfo> getLocation(String ip) {
        if (reader == null) {
            return Optional.empty();
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            return Optional.of(new LocationInfo(
                response.getCountry().getName(),
                response.getCountry().getIsoCode(),
                response.getCity().getName(),
                response.getSubdivisions().isEmpty() ? null : response.getSubdivisions().get(0).getName(),
                response.getLocation().getLatitude(),
                response.getLocation().getLongitude(),
                ipAddress.getHostAddress()
            ));
        } catch (IOException | GeoIp2Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 地理位置信息封装类
     */
    public static class LocationInfo {
        private String country;      // 国家名称
        private String countryCode;  // 国家代码
        private String city;         // 城市名称
        private String province;     // 省/州
        private Double latitude;     // 纬度
        private Double longitude;    // 经度

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        private String ip;

        public LocationInfo(String country, String countryCode, String city, 
                            String province, Double latitude, Double longitude, String ip) {
            this.country = country;
            this.countryCode = countryCode;
            this.city = city;
            this.province = province;
            this.latitude = latitude;
            this.longitude = longitude;
            this.ip = ip;
        }

        // Getters
        public String getCountry() { return country; }
        public String getCountryCode() { return countryCode; }
        public String getCity() { return city; }
        public String getProvince() { return province; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }

        @Override
        public String toString() {
            if (province == null) {
                 return String.format("%s(%s)",
                        country , countryCode);
            }
            if (city == null) {
                return String.format("%s(%s) - %s",
                        country , countryCode, province);
            }
            return String.format("%s(%s) - %s, %s",
                country , countryCode, province, city);
        }
    }

    // 使用示例
    public static void main(String[] args) {
        IPGeoUtil geoUtil = IPGeoUtil.getInstance();

        // 测试几个不同的IP
        String[] testIPs = {
            "8.8.8.8",     // Google DNS (US)
            "1.1.1.1",     // Cloudflare DNS (US)
            "114.114.114.114"  // 国内DNS
        };

        for (String ip : testIPs) {
            Optional<LocationInfo> location = geoUtil.getLocation(ip);
            if (location.isPresent()) {
                System.out.println(ip + " 位置: " + location.get());
            } else {
                System.out.println(ip + " 无法解析位置");
            }
        }
    }
}