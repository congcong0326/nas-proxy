package org.congcong.nasproxy.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyConfig {

    @JsonProperty("inbounds")
    private List<Inbound> inbounds;

    @JsonProperty("outbounds")
    private List<Outbound> outbounds;
}
