package org.congcong.nasproxy.web.controller;

import lombok.Data;
import org.congcong.nasproxy.common.entity.DiskDetail;
import org.congcong.nasproxy.common.entity.DiskInfo;
import org.congcong.nasproxy.common.entity.PcStatus;
import org.congcong.nasproxy.common.entity.UserAccessEntity;
import org.congcong.nasproxy.core.monitor.connect.ConnectionMonitor;
import org.congcong.nasproxy.core.monitor.connect.IConnectionMonitor;
import org.congcong.nasproxy.core.monitor.db.PartitionType;
import org.congcong.nasproxy.core.monitor.disk.DiskService;
import org.congcong.nasproxy.core.monitor.serverResource.ISystemMonitor;
import org.congcong.nasproxy.core.monitor.serverResource.SystemMonitor;
import org.congcong.nasproxy.core.monitor.traffic.ITrafficAnalysis;
import org.congcong.nasproxy.core.monitor.traffic.TrafficReportService;
import org.congcong.nasproxy.core.monitor.wol.IpMonitor;
import org.congcong.nasproxy.common.config.WolConfig;
import org.congcong.nasproxy.core.monitor.wol.WolService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*") // 允许所有来源访问这个Controller
@RestController
@RequestMapping("/api")
public class MonitorController {
    
    private final ITrafficAnalysis trafficAnalysis = TrafficReportService.getInstance();
    private final ISystemMonitor systemMonitor = SystemMonitor.getInstance();
    private final IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();
    private final WolService wolService = WolService.getInstance();
    private final DiskService diskService = DiskService.getInstance();


    // 系统概览数据
    @GetMapping("/overview")
    public ResponseEntity<SystemOverviewDTO> getOverview() {
        SystemOverviewDTO dto = new SystemOverviewDTO();

        // 电脑在线信息
        List<PcStatus> allPcStatus = IpMonitor.getInstance().getAllPcStatus();
        dto.setPcs(allPcStatus);
        // 系统监控
        dto.setCpuUsage(systemMonitor.getCpuUsage());
        dto.setMemoryUsage(systemMonitor.getSystemMemoryUsage());
        dto.setJvmMemoryUsage(systemMonitor.getJvmMemoryUsage());
        
        // 网络连接
        dto.setTcpConnections(connectionMonitor.currentTcpConnections());
        dto.setSuccessRate(connectionMonitor.connectionSuccessRate());
        
        // 流量数据
        long currentTime = System.currentTimeMillis();
        dto.setMonthlyUp(trafficAnalysis.getTotalUpTraffic(currentTime, PartitionType.MONTH));
        dto.setMonthlyDown(trafficAnalysis.getTotalDownTraffic(currentTime, PartitionType.MONTH));
        
        return ResponseEntity.ok(dto);
    }

    // 每日详情
    @GetMapping("/daily")
    public ResponseEntity<DailyTrafficDTO> getDailyDetails(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        
        DailyTrafficDTO dto = new DailyTrafficDTO();
        long timestamp = date.getTime();
        
        dto.setUp(trafficAnalysis.getTotalUpTraffic(timestamp, PartitionType.DAY));
        dto.setDown(trafficAnalysis.getTotalDownTraffic(timestamp, PartitionType.DAY));
        dto.setTopUsers(trafficAnalysis.getTop10Traffic(timestamp, PartitionType.DAY));
        
        return ResponseEntity.ok(dto);
    }

    // 用户详情
    @GetMapping("/user")
    public ResponseEntity<UserDetailDTO> getUserDetails(
            @RequestParam String user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        
        UserDetailDTO dto = new UserDetailDTO();
        List<UserAccessEntity> details = trafficAnalysis.getUserAccessDetail(date.getTime(), user);
        dto.setAccessRecords(details);
        
        return ResponseEntity.ok(dto);
    }

    // WOL唤醒
    @GetMapping("/wol")
    public ResponseEntity<String> wakeOnLan(@RequestParam String ip) {
        WolConfig.WolEntity entity = IpMonitor.getInstance().getByIp(ip);
        if (entity == null) {
            return ResponseEntity.badRequest().body("Device not found");
        }
        return ResponseEntity.ok(wolService.sendWolPacket(
            entity.getMacAddress(),
            entity.getSubNetMask(),
            entity.getWolPort()
        ));
    }

    @GetMapping("/disk/list")
    public List<DiskInfo> getAllDisks() {
        return diskService.getAllDisks();
    }

    @GetMapping("/disk/detail/{device}")
    public DiskDetail getDiskDetail(@PathVariable String device) {
        return diskService.getDiskDetail(device.replace("dev/", ""));
    }

    // DTO定义
    @Data
    static class SystemOverviewDTO {
        // name ip  online
        private List<PcStatus> pcs;
        private double cpuUsage;
        private String memoryUsage;
        private String jvmMemoryUsage;
        private int tcpConnections;
        private double successRate;
        private Long monthlyUp;
        private Long monthlyDown;
    }



    @Data
    static class DailyTrafficDTO {
        private Long up;
        private Long down;
        private Map<String, Long> topUsers;
    }

    @Data
    static class UserDetailDTO {
        private List<UserAccessEntity> accessRecords;
    }
}