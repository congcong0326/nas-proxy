package org.congcong.nasproxy.common.util;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class TimeUtil {

    /**
     * 返回从今天到月初每天的时间戳列表。
     * @param timestamp 给定的时间戳
     * @return 从今天到月初每天相同时间的时间戳列表
     */
    public static List<Long> getTimestampsFromTodayToMonthStart(long timestamp) {
        // 将时间戳转换为 LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

        // 获取当前日期和本月的第一天
        LocalDate today = dateTime.toLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);

        // 用于存储结果的时间戳列表
        List<Long> timestamps = new ArrayList<>();

        // 从今天开始，逐天往回直到月初
        for (LocalDate date = today; !date.isBefore(monthStart); date = date.minusDays(1)) {
            // 获取当天的00:00:00时间戳
            LocalDateTime dayAtMidnight = date.atStartOfDay();
            long millis = dayAtMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            timestamps.add(millis);
        }

        return timestamps;
    }

    public static boolean isSameDay(long timestamp1, long timestamp2) {
        // 获取两个时间戳对应的日期
        ZonedDateTime date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault());
        ZonedDateTime date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault());

        // 比较年、月、日是否相同
        return date1.getYear() == date2.getYear() &&
                date1.getMonthValue() == date2.getMonthValue() &&
                date1.getDayOfMonth() == date2.getDayOfMonth();
    }

    public static boolean isWithinOneHour(long timestamp1, long timestamp2) {
        // 计算时间差，取绝对值
        long timeDifference = Math.abs(timestamp1 - timestamp2);

        // 1 小时 = 3600000 毫秒
        return timeDifference <= 3600000;
    }

    public static long truncateToHour(long timestamp) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
        // 将分钟、秒、纳秒归零
        ZonedDateTime truncated = dateTime.withMinute(0).withSecond(0).withNano(0);
        // 转回时间戳
        return truncated.toInstant().toEpochMilli();
    }
}
