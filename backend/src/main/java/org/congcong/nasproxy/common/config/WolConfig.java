package org.congcong.nasproxy.common.config;

import java.util.List;

public class WolConfig {


    public List<WolEntity> getWolConfig() {
        return wolConfig;
    }

    public void setWolConfig(List<WolEntity> wolConfig) {
        this.wolConfig = wolConfig;
    }

    private List<WolEntity> wolConfig;

    public static class WolEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getSubNetMask() {
            return subNetMask;
        }

        public void setSubNetMask(String subNetMask) {
            this.subNetMask = subNetMask;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public int getWolPort() {
            return wolPort;
        }

        public void setWolPort(int wolPort) {
            this.wolPort = wolPort;
        }

        private String ipAddress;
        private String subNetMask;
        private String macAddress;
        private int wolPort;




    }
}
