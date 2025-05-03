package org.congcong.nasproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.common.util.ConfigReader;
import org.junit.jupiter.api.Test;

@Slf4j
public class ConfigTest {

    @Test
    public void aVoid() throws Exception {
        ProxyConfig config = ConfigReader.loadProxyConfig("src/main/resources/config.json");
        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(config));
    }
}
