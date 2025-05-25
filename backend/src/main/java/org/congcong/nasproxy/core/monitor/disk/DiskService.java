package org.congcong.nasproxy.core.monitor.disk;


import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.WolConfig;
import org.congcong.nasproxy.common.entity.DiskDetail;
import org.congcong.nasproxy.common.entity.DiskInfo;
import org.congcong.nasproxy.common.exception.DeviceNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DiskService {

    private static final DiskService INSTANCE = new DiskService();

    private DiskService() {}

    public static DiskService getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, List<Integer>> temperatureInfo = new HashMap<>();
    private final Map<String, LocalDate> lastUpdateDate = new HashMap<>();
    private final List<String> devices = new ArrayList<>();

    private boolean isSupport = false;

    public void start() {
        isSupport = isSupport();
        if (!isSupport) {
            return;
        }
        devices.addAll(getPhysicalDisks());
        scheduler.scheduleAtFixedRate(() -> {
            LocalDate nowDate = LocalDate.now();
            int currentSlot = LocalTime.now().getHour() * 6 + LocalTime.now().getMinute() / 10;

            for (String device : devices) {
                try {
                    int temperature = getTemperature(device);
                    List<Integer> temps = temperatureInfo.computeIfAbsent(device, k -> new ArrayList<>());
                    LocalDate lastDate = lastUpdateDate.get(device);

                    // 如果是新的一天，重置list
                    if (lastDate == null || !lastDate.equals(nowDate)) {
                        temps.clear();
                        lastUpdateDate.put(device, nowDate);
                    }

                    // 补0直到当前位置
                    while (temps.size() < currentSlot) {
                        temps.add(0);
                    }

                    if (temps.size() == currentSlot) {
                        temps.add(temperature); // 正常采样
                    } else {
                        temps.set(currentSlot, temperature); // 覆盖（异常情况）
                    }
                } catch (Exception e) {
                    log.error("获取磁盘温度失败", e);
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private int getTemperature(String device) {
        String dataOutput = executeSmartCtl(device, "-A");
        return SmartCtlParser.parseTemperature(dataOutput);
    }

    public List<DiskInfo> getAllDisks() {
        if (!isSupport) {
            return Collections.emptyList();
        }
        return devices.stream()
            .map(this::getDiskInfo)
            .toList();
    }

    public DiskDetail getDiskDetail(String device) {
        if (!devices.contains(device)) {
            throw new DeviceNotFoundException("Device not found");
        }
        return SmartCtlParser.parseDetail(device, executeSmartCtl(device, "-a"), temperatureInfo.get(device));
    }

    private DiskInfo getDiskInfo(String device) {
        String infoOutput = executeSmartCtl(device, "-i");
        String healthOutput = executeSmartCtl(device, "-H");
        String dataOutput = executeSmartCtl(device, "-A");
        return SmartCtlParser.parseBasicInfo(device, infoOutput, healthOutput, dataOutput);
    }


    private String executeSmartCtl(String device, String option) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/sbin/smartctl", option, "/dev/" + device);
            pb.redirectErrorStream(true);  // 合并 stderr
            Process process = pb.start();

            // 自动关闭 BufferedReader，避免资源泄露
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                boolean success = process.waitFor(5, TimeUnit.SECONDS);
                if (!success || process.exitValue() != 0) {
                    throw new RuntimeException("smartctl exited with code: " + process.exitValue()
                            + "\nOutput:\n" + output);
                }

                return output.toString();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Smartctl execution failed", e);
        }
    }



    private static List<String> getPhysicalDisks() {
        List<String> physicalDisks = new ArrayList<>();

        try {
            Process process = new ProcessBuilder("lsblk", "-o", "NAME,TYPE,SIZE,MODEL").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    // 跳过标题行
                    headerSkipped = true;
                    continue;
                }

                // 去掉行首空格并按空白分割
                String[] parts = line.trim().split("\\s+");

                // 合法的硬盘行一般会有 4 个字段：NAME TYPE SIZE MODEL
                if (parts.length >= 4) {
                    String name = parts[0];
                    String type = parts[1];
                    String model = parts[3];

                    if ("disk".equals(type) && !model.equals("") && !model.equalsIgnoreCase("null")) {
                        physicalDisks.add(name);
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return physicalDisks;
    }


    private static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux");
    }

    private static boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder("which", command).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSupport() {
        if (!isLinux()) {
            log.warn("not linux, can not support disk monitor");
            return false;
        }

        boolean smartctlExists = isCommandAvailable("smartctl");
        boolean lsblkExists = isCommandAvailable("lsblk");

        if (smartctlExists && lsblkExists) {
            return true;
        } else {
            if (!smartctlExists) log.warn("need - smartctl");
            if (!lsblkExists) log.warn("need - lsblk");
            return false;
        }
    }

}