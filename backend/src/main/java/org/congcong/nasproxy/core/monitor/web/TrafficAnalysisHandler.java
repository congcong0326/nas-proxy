//package org.congcong.nasproxy.core.monitor.web;
//
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.handler.codec.http.*;
//import io.netty.util.CharsetUtil;
//import org.congcong.multi.proxy.common.Const;
//import org.congcong.multi.proxy.common.SingletonUtil;
//import org.congcong.multi.proxy.entity.AttackLog;
//import org.congcong.multi.proxy.entity.UserAccessEntity;
//import org.congcong.multi.proxy.monitor.connect.ConnectionMonitor;
//import org.congcong.multi.proxy.monitor.connect.IConnectionMonitor;
//import org.congcong.multi.proxy.monitor.connect.IPGeoUtil;
//import org.congcong.multi.proxy.monitor.db.PartitionType;
//import org.congcong.multi.proxy.monitor.serverResource.ISystemMonitor;
//import org.congcong.multi.proxy.monitor.serverResource.SystemMonitor;
//import org.congcong.multi.proxy.monitor.traffic.ITrafficAnalysis;
//import org.congcong.multi.proxy.monitor.traffic.TrafficReportService;
//import org.congcong.multi.proxy.monitor.wol.IpMonitor;
//import org.congcong.multi.proxy.monitor.wol.WolConfig;
//import org.congcong.multi.proxy.monitor.wol.WolConfigProvider;
//import org.congcong.multi.proxy.monitor.wol.WolService;
//
//import java.io.UnsupportedEncodingException;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class TrafficAnalysisHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
//
//    private static final ITrafficAnalysis trafficAnalysis = SingletonUtil.getInstance(TrafficReportService.class);
//
//    private static final ISystemMonitor systemMonitor = SingletonUtil.getInstance(SystemMonitor.class);
//
//    private static final IConnectionMonitor connectionMonitor = SingletonUtil.getInstance(ConnectionMonitor.class);
//
//
//    private static final IpMonitor ipMonitor = SingletonUtil.getInstance(IpMonitor.class);
//
//    private static final WolService wol = SingletonUtil.getInstance(WolService.class);
//
//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
//        String uri = request.uri();
//        String responseContent;
//
//        if (uri.startsWith("/details")) {
//            String date = getQueryParameter(uri, "date");
//            responseContent = getDayDetailsPage(date);
//        } else if (uri.startsWith("/user")) {
//            String date = getQueryParameter(uri, "date");
//            String user = getQueryParameter(uri, "user");
//            responseContent = getUserDetailPage(date, user);
//        } else if (uri.startsWith("/wol")) {  // 新增WOL处理
//            Map<String, String> params = parseQueryParameters(uri);
//            WolConfig.WolEntity wolEntity = WolConfigProvider.getByIp(params.get("ip"));
//            if (wolEntity == null) {
//                responseContent = "未找到相关配置";
//            } else {
//                String sendWolPacket = wol.sendWolPacket(
//                        wolEntity.getMacAddress(),
//                        wolEntity.getSubNetMask(),
//                        wolEntity.getWolPort());
//                responseContent = sendWolPacket;
//            }
//        } else {
//            responseContent = getHomePage();
//        }
//
//        FullHttpResponse response = new DefaultFullHttpResponse(
//                HttpVersion.HTTP_1_1,
//                HttpResponseStatus.OK,
//                io.netty.buffer.Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8)
//        );
//        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
//        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
//        response.headers().set(HttpHeaderNames.PRAGMA, "no-cache");
//        response.headers().set(HttpHeaderNames.EXPIRES, "0");
//        ctx.writeAndFlush(response).addListener(future -> ctx.close());
//    }
//
//    private Map<String, String> parseQueryParameters(String uri) {
//        return Arrays.stream(uri.split("\\?")[1].split("&"))
//                .map(param -> param.split("="))
//                .collect(Collectors.toMap(
//                        arr -> arr[0],
//                        arr -> {
//                            try {
//                                return arr.length > 1 ? URLDecoder.decode(arr[1], StandardCharsets.UTF_8.toString()) : "";
//                            } catch (UnsupportedEncodingException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                ));
//    }
//
//
//    private String getHomePage() {
//        long currentTime = System.currentTimeMillis();
//        Long totalUp = trafficAnalysis.getTotalUpTraffic(currentTime, PartitionType.MONTH);
//        Long totalDown = trafficAnalysis.getTotalDownTraffic(currentTime, PartitionType.MONTH);
//
//        // 获取系统监控数据
//        double cpuUsage = systemMonitor.getCpuUsage();
//        String memoryUsage = systemMonitor.getSystemMemoryUsage();
//        String jvmMemoryUsage = systemMonitor.getJvmMemoryUsage();
//        String diskUsage = systemMonitor.getDiskUsage();
//        String processRunDetail = systemMonitor.processRunDetail();
//        int tcpConnections = connectionMonitor.currentTcpConnections();
//        double successRate = connectionMonitor.connectionSuccessRate();
//        int connectionCount = connectionMonitor.connectionCount();
//        int connectionFailed = connectionMonitor.connectionFailed();
//        Map<String, Long> suspiciousIPAddress = connectionMonitor.suspiciousIPAddress(System.currentTimeMillis(), PartitionType.MONTH);
//        Map<String, Long> addressAndAccessCount = connectionMonitor.ipAddressAnalysis(System.currentTimeMillis(), PartitionType.MONTH, 20);
//        List<AttackLog> attackLog = connectionMonitor.getAttackLog(System.currentTimeMillis(), PartitionType.MONTH);
//        WolConfig wolConfig = WolConfigProvider.getConfig();
//
//        StringBuilder builder = new StringBuilder();
//        // 展示系统监控数据
//        // 展示网络连接
//        builder.append("<!DOCTYPE html>")
//                .append("<html><head><title>仪表盘</title></head>")
//                .append("<body>")
//                .append("<h1>仪表盘</h1>");
//        // wol
//        builder.append("<h2>wake on lan</h2>");
//        if (wolConfig != null && wolConfig.getWolConfig() != null) {
//            List<String> ipList = wolConfig.getWolConfig().stream().map(WolConfig.WolEntity::getIpAddress).filter(Objects::nonNull).collect(Collectors.toList());
//            ipMonitor.start(ipList);
//            for (WolConfig.WolEntity wolEntity : wolConfig.getWolConfig()) {
//                String ip = wolEntity.getIpAddress();
//                boolean isOnline = ipMonitor.isOnline(ip);
//
//                builder.append("<div style='margin: 20px; padding: 10px; border: 1px solid #ccc;'>")
//                        .append("<h3>").append(wolEntity.getName()).append("</h3>")
//                        .append("<p>状态: ")
//                        .append(isOnline ?
//                                "<span style='color: green;'>在线</span>" :
//                                "<span style='color: red;'>离线</span>")
//                        .append("</p>")
//                        .append("<p>IP: ").append(ip).append("</p>")
//                        .append("<p>MAC: ").append(wolEntity.getMacAddress()).append("</p>");
//
//                if (!isOnline) {
//                    builder.append("<td><a href='/wol?ip=").append(ip).append("'>").append("唤醒").append(ip).append("</a></td>");
//                }
//                builder.append("</div>");
//            }
//        }
//
//
//
//
//
//                // 展示系统监控数据
//        builder.append("<h2>系统监控</h2>")
//                .append("<p>CPU 使用率: ").append(String.format("%.2f", cpuUsage)).append("%</p>")
//                .append("<p>系统内存使用情况: ").append(memoryUsage).append("</p>")
//                .append("<p>程序内存使用情况: ").append(jvmMemoryUsage).append("</p>")
//                .append("<p>磁盘使用情况: ").append(diskUsage).append("</p>")
//                .append("<p>程序运行时间: ").append(processRunDetail).append("</p>")
//
//                // 展示网络连接
//                .append("<h2>网络连接</h2>")
//                .append("<p>当前 TCP 连接数: ").append(tcpConnections).append("</p>")
//                .append("<p>连接目标地址成功率: ").append(successRate * 100).append("%").append("[失败").append(connectionFailed).append("次]")
//                .append("[总共").append(connectionCount).append("次]")
//                .append("</p>");
//        if (!suspiciousIPAddress.isEmpty()) {
//            builder.append("<h2>解密失败的 IP 地址(有可能是重放攻击)</h2>")
//                    .append("<table border='1'><tr><th>IP 地址</th><th>访问次数</th></tr>");
//            for (Map.Entry<String, Long> entry : suspiciousIPAddress.entrySet()) {
//                builder.append("<tr><td>").append(entry.getKey()).append("</td>")
//                        .append("<td>").append(entry.getValue()).append("</td></tr>");
//            }
//            builder.append("</table>");
//        }
//
//
//        if (attackLog != null) {
//            builder.append("<h2>攻击日志</h2>")
//                    .append("<table border='1'>")
//                    .append("<tr><th>IP 地址</th><th>原因及应对措施</th><th>开始时间</th><th>持续时间</th><th>接收字节数</th><th>发送字节数</th><th>端口</th></tr>");
//            IPGeoUtil ipUtil = IPGeoUtil.getInstance();
//
//            for (AttackLog attack : attackLog) {
//                Optional<IPGeoUtil.LocationInfo> location = ipUtil.getLocation(attack.getIp());
//                String address = location.isPresent() ? location.get().toString() : "";
//                builder.append("<tr>")
//                        .append("<td>").append(attack.getIp()).append("/").append(address).append("</td>")
//                        .append("<td>").append(Const.translateAttackType(attack.getAttackType())).append("</td>")
//                        .append("<td>").append(AttackLog.formatTime(attack.getStartTime())).append("</td>")
//                        .append("<td>").append((attack.getLastTime() - attack.getStartTime()) / 1000).append("ms</td>")  // assuming `lastTime` is in milliseconds
//                        .append("<td>").append(attack.getReceiveBytes()).append("</td>")
//                        .append("<td>").append(attack.getSendBytes()).append("</td>")
//                        .append("<td>").append(attack.getPort()).append("</td>")
//                        .append("</tr>");
//            }
//            builder.append("</table>");
//        }
//
//        // 展示访问的 IP 地址统计
//        builder.append("<h2>访问 IP 地址统计</h2>")
//                .append("<table border='1'><tr><th>IP 地址</th><th>访问次数</th></tr>");
//
//        // 将 IP 地址和访问次数添加到表格中
//        for (Map.Entry<String, Long> entry : addressAndAccessCount.entrySet()) {
//            builder.append("<tr><td>").append(entry.getKey()).append("</td>")
//                    .append("<td>").append(entry.getValue()).append("</td></tr>");
//        }
//
//        builder.append("</table>")
//
//                // 展示流量数据
//                .append("<h2>流量分析</h2>")
//                .append("<p>本月上行流量: ").append(formatTraffic(totalUp)).append("</p>")
//                .append("<p>本月下行流量: ").append(formatTraffic(totalDown)).append("</p>")
//
//                // 展示日历
//                .append("<div id='calendar'><h2>选择日期查看访问详情</h2>")
//                .append(getMonthlyCalendar())
//                .append("</div>")
//                .append("</body></html>");
//
//        return builder.toString();
//    }
//
//    private String getMonthlyCalendar() {
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.DAY_OF_MONTH, 1);
//        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
//        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("<table border='1'><tr>");
//
//        // 渲染星期几的标题
//        String[] daysOfWeek = {"日", "一", "二", "三", "四", "五", "六"};
//        for (String day : daysOfWeek) {
//            builder.append("<th>").append(day).append("</th>");
//        }
//        builder.append("</tr><tr>");
//
//        // 渲染前面的空单元格
//        for (int i = 1; i < firstDayOfWeek; i++) {
//            builder.append("<td></td>");
//        }
//
//        // 渲染日期并添加链接
//        for (int day = 1; day <= daysInMonth; day++) {
//            if ((day + firstDayOfWeek - 2) % 7 == 0 && day != 1) {
//                builder.append("</tr><tr>");
//            }
//
//            String date = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, day);
//            builder.append("<td><a href='/details?date=").append(date).append("'>").append(day).append("</a></td>");
//        }
//
//        // 渲染后面的空单元格
//        int lastDayOfWeek = (daysInMonth + firstDayOfWeek - 1) % 7;
//        if (lastDayOfWeek != 0) {
//            for (int i = lastDayOfWeek; i < 7; i++) {
//                builder.append("<td></td>");
//            }
//        }
//
//        builder.append("</tr></table>");
//        return builder.toString();
//    }
//
//    private String getDayDetailsPage(String date) {
//        long currentTime = parseDate(date);
//        Long dailyUp = trafficAnalysis.getTotalUpTraffic(currentTime, PartitionType.DAY);
//        Long dailyDown = trafficAnalysis.getTotalDownTraffic(currentTime, PartitionType.DAY);
//        Map<String, Long> top10Traffic = trafficAnalysis.getTop10Traffic(currentTime, PartitionType.DAY);
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("<!DOCTYPE html>")
//                .append("<html><head><title>每日流量详情</title></head>")
//                .append("<body><h1>").append(date).append(" 的流量详情</h1>")
//                .append("<p>每日上行流量: ").append(formatTraffic(dailyUp)).append("</p>")
//                .append("<p>每日下行流量: ").append(formatTraffic(dailyDown)).append("</p>")
//                .append("<h2>用户流量排名</h2>");
//
//        for (Map.Entry<String, Long> entry : top10Traffic.entrySet()) {
//            builder.append("<a href='/user?date=").append(date).append("&user=").append(entry.getKey()).append("'>")
//                    .append(entry.getKey()).append(": ").append(formatTraffic(entry.getValue())).append("</a><br>");
//        }
//
//        builder.append("</body></html>");
//        return builder.toString();
//    }
//
//    private String getUserDetailPage(String date, String user) {
//        long currentTime = parseDate(date);
//        List<UserAccessEntity> userAccessDetail = trafficAnalysis.getUserAccessDetail(currentTime, user);
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("<!DOCTYPE html>")
//                .append("<html><head><title>用户流量详情</title></head>")
//                .append("<body><h1>").append(user).append(" 在 ").append(date).append(" 的流量详情</h1>")
//                .append("<table><tr><th>URL</th><th>访问次数</th><th>上行流量</th><th>下行流量</th><th>客户端 IP</th><th>时长 (秒)</th><th>最后访问时间</th></tr>");
//
//        for (UserAccessEntity entry : userAccessDetail) {
//            builder.append("<tr><td>").append(entry.getRemoteUrl()).append("</td>")
//                    .append("<td>").append(entry.getAccessCount()).append("</td>")
//                    .append("<td>").append(formatTraffic(entry.getUp())).append("</td>")
//                    .append("<td>").append(formatTraffic(entry.getDown())).append("</td>")
//                    .append("<td>").append(entry.getClientIp()).append("</td>")
//                    .append("<td>").append(formatTime(entry.getDurationTime())).append("</td>")
//                    .append("<td>").append(convertLongToDate(entry.getLastAccessTime())).append("</td>")
//                    .append("</tr>");
//        }
//
//        builder.append("</table></body></html>");
//        return builder.toString();
//    }
//
//    private long parseDate(String date) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//            return sdf.parse(date).getTime();
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Invalid date format");
//        }
//    }
//
//    private static String convertLongToDate(long timestamp) {
//        // 创建一个 SimpleDateFormat 实例，指定日期格式
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        // 将 long 类型时间戳转化为 Date 对象
//        Date date = new Date(timestamp);
//        // 使用 sdf 格式化日期
//        return sdf.format(date);
//    }
//
//    private String getQueryParameter(String uri, String parameter) {
//        String[] parts = uri.split("\\?");
//        if (parts.length < 2) return null;
//
//        for (String param : parts[1].split("&")) {
//            String[] kv = param.split("=");
//            if (kv.length == 2 && kv[0].equals(parameter)) {
//                return kv[1];
//            }
//        }
//        return null;
//    }
//
//    private String formatTime(int time) {
//        if (time > 1000) {
//            return (time / 1000) + "s";
//        }
//        return time + "ms";
//    }
//
//    private String formatTraffic(long bytes) {
//        if (bytes < 1024) {
//            return bytes + " Byte";
//        } else if (bytes < 1024 * 1024) {
//            return String.format("%.2f KB", bytes / 1024.0);
//        } else if (bytes < 1024 * 1024 * 1024) {
//            return String.format("%.2f MB", bytes / (1024.0 * 1024));
//        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
//            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
//        } else {
//            return String.format("%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
//        }
//    }
//
//
//}
