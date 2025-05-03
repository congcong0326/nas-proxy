package org.congcong.nasproxy.core.monitor.wol;

import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.WolConfig;
import org.congcong.nasproxy.common.entity.PcStatus;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class IpMonitor {


    private static final IpMonitor INSTANCE = new IpMonitor();

    private IpMonitor() {}

    public static IpMonitor getInstance() {
        return INSTANCE;
    }

    private final Map<String, Boolean> statusMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<String> ipList;

    private WolConfig wolConfig;
    public void start(WolConfig wolConfig) {
        this.wolConfig = wolConfig;
        // 每5秒执行一次检测任务
        this.ipList =  wolConfig.getWolConfig().stream().map(WolConfig.WolEntity::getIpAddress).collect(Collectors.toList());
        scheduler.scheduleAtFixedRate(() -> {
            for (String ip : ipList) {
                try {
                    boolean isOnline = InetAddress.getByName(ip).isReachable(1000); // 1秒超时
                    statusMap.put(ip, isOnline);
                } catch (Exception e) {
                    statusMap.put(ip, false); // 异常视为离线
                }
            }
        }, 0, 5, TimeUnit.SECONDS);

    }

    public WolConfig.WolEntity getByIp(String ip) {
        List<WolConfig.WolEntity> wolConfigList = wolConfig.getWolConfig();
        for (WolConfig.WolEntity wolEntity : wolConfigList) {
            if (wolEntity.getIpAddress().equals(ip)) {
                return wolEntity;
            }
        }
        return null;
    }

    public List<PcStatus> getAllPcStatus() {
        List<PcStatus> result = new ArrayList<>();
        for (WolConfig.WolEntity wolEntity : wolConfig.getWolConfig()) {
            String ipAddress = wolEntity.getIpAddress();
            Boolean online = isOnline(ipAddress);
            PcStatus pcStatus = new PcStatus();
            pcStatus.setName(wolEntity.getName());
            pcStatus.setOnline(online);
            pcStatus.setIp(wolEntity.getIpAddress());
            result.add(pcStatus);
        }
        return result;
    }

    private Boolean isOnline(String ip) {
        Boolean aBoolean = statusMap.get(ip);
        if (aBoolean == null) {
            return false;
        }
        return aBoolean;
    }





}
