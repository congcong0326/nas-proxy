package org.congcong.nasproxy.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.congcong.nasproxy.common.entity.ProtocolType;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Outbound {
    @JsonProperty("tag")
    private String tag;

    @JsonProperty("port")
    private int port;

    @JsonProperty("protocol")
    private ProtocolType protocol;

    @JsonProperty("settings")
    private Map<String, Object> settings;

    @JsonProperty("user")
    private String user;
}
