package org.congcong.nasproxy.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.common.config.WolConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Slf4j
public class ConfigReader {

    public static <T> T loadConfig(String fileName, Class<T> configClass) throws Exception {
        String jarDir = System.getProperty("user.dir");
        File configFile = new File(jarDir, fileName);
        ObjectMapper mapper = new ObjectMapper();

        if (configFile.exists()) {
            return mapper.readValue(configFile, configClass);
        } else {
            try (InputStream in = configClass.getClassLoader().getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new FileNotFoundException("Configuration file not found: " + fileName);
                }
                return mapper.readValue(in, configClass);
            }
        }
    }

    public static ProxyConfig loadProxyConfig(String fileName) throws Exception {
        return loadConfig(fileName, ProxyConfig.class);
    }

    public static WolConfig loadWolConfig(String fileName) throws Exception {
        return loadConfig(fileName, WolConfig.class);
    }

}
