package org.congcong.nasproxy.core.monitor.disk;

import org.congcong.nasproxy.common.entity.DiskDetail;
import org.congcong.nasproxy.common.entity.DiskInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SmartCtlParser {
    private static final Pattern DEVICE_MODEL = Pattern.compile("Device Model:\\s+(.+)");
    private static final Pattern SERIAL_NUMBER = Pattern.compile("Serial Number:\\s+(.+)");
    private static final Pattern USER_CAPACITY = Pattern.compile("User Capacity:\\s+[\\d,]+ bytes \\[(.+?)]");
    private static final Pattern TEMPERATURE = Pattern.compile(
            "^194\\s+Temperature_Celsius\\s+(?:\\S+\\s+){7}(\\d+)(?:\\D.*)?",
            Pattern.MULTILINE
    );
    private static final Pattern HEALTH_STATUS = Pattern.compile("SMART overall-health self-assessment test result: (\\w+)");
    private static final Pattern SMART_SUPPORTED = Pattern.compile("SMART support is:\\s+Available (\\w+)");
    private static final Pattern SMART_ENABLED = Pattern.compile("SMART support is:\\s+Enabled");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s+(\\S+)\\s+\\S+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\S+\\s+\\S+\\s+\\S+\\s+(\\S+)", Pattern.MULTILINE);
    public static DiskInfo parseBasicInfo(String device, String infoOutput, String healthOutput, String dataOutput) {
        return new DiskInfo(
                "/dev/" + device,
                extractGroup(infoOutput, DEVICE_MODEL),
                extractGroup(infoOutput, SERIAL_NUMBER),
                extractSize(infoOutput),
                parseHealthStatus(healthOutput),
                parseTemperature(dataOutput)
        );
    }

/*    public static int parseTemperature(String dataOutput) {
        return parseTemperature(dataOutput);
    }*/

    public static DiskDetail parseDetail(String device, String fullOutput, List<Integer> historyTemperature) {
        Map<String, Integer> attributes = parseAttributes(fullOutput);

        return new DiskDetail(
                device,
                extractGroup(fullOutput, DEVICE_MODEL),
                extractGroup(fullOutput, SERIAL_NUMBER),
                extractSize(fullOutput),
                parseTemperature(fullOutput),
                extractGroup(fullOutput, HEALTH_STATUS),
                isSmartSupported(fullOutput),
                isSmartEnabled(fullOutput),
                attributes.getOrDefault("Power_On_Hours", 0),
                attributes.getOrDefault("Power_Cycle_Count", 0),
                attributes.getOrDefault("Reallocated_Sector_Ct", 0),
                attributes.getOrDefault("Seek_Error_Rate", 0),
                attributes.getOrDefault("Spin_Retry_Count", 0),
                attributes.getOrDefault("UDMA_CRC_Error_Count", 0),
                historyTemperature
        );
    }

    private static String extractGroup(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1).trim() : "N/A";
    }

    private static String extractSize(String input) {
        Matcher matcher = USER_CAPACITY.matcher(input);
        return matcher.find() ? matcher.group(1) : "N/A";
    }

    private static String parseHealthStatus(String healthOutput) {
        Matcher matcher = HEALTH_STATUS.matcher(healthOutput);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }

    public static int parseTemperature(String dataOutput) {
        Matcher matcher = TEMPERATURE.matcher(dataOutput);
        try {
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        } catch (Exception e) {

        }
        return 0;
    }

    private static boolean isSmartSupported(String input) {
        return input.contains("SMART support is: Available");
    }

    private static boolean isSmartEnabled(String input) {
        return input.contains("SMART support is: Enabled");
    }

    private static Map<String, Integer> parseAttributes(String input) {
        Map<String, Integer> attributes = new HashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(input);

        while (matcher.find()) {
            String attributeName = matcher.group(2).trim();
            String rawValueStr = matcher.group(3).trim();

            // 去掉括号内的说明内容，例如 "273 (Average 406)" -> "273"
            rawValueStr = rawValueStr.replaceAll("\\(.*?\\)", "").trim();

            try {
                long rawValue = Long.parseLong(rawValueStr);
                attributes.put(attributeName, (int) Math.min(rawValue, Integer.MAX_VALUE)); // 限制为int范围
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的数值
            }
        }
        return attributes;
    }



}