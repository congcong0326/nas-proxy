package org.congcong.nasproxy.core.monitor.wol;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


public class WolService {

    private static final WolService INSTANCE = new WolService();

    private WolService() {}

    public static WolService getInstance() {
        return INSTANCE;
    }


    public String sendWolPacket(String macAddress, String broadcastIp, int port) {
        String result; // 直接使用 String 变量
        EventLoopGroup group = new NioEventLoopGroup();
        Channel channel = null; // Declare channel outside try to ensure it's closed in finally if opened
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true) // 启用广播
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                            // 不需要处理响应
                        }
                    });

            channel = bootstrap.bind(0).sync().channel(); // Bind and wait for bind to complete

            ByteBuf wolBuffer = buildWolPacket(macAddress); // Assumes buildWolPacket is defined

            // 发送到广播地址
            InetAddress address = InetAddress.getByName(broadcastIp);

            // Write and flush, then wait for the write to complete
            channel.writeAndFlush(new DatagramPacket(wolBuffer, new InetSocketAddress(address, port))).sync();

            // If sync() didn't throw an exception, the packet was sent successfully
            result = "WOL魔术包发送成功";

        } catch (UnknownHostException | InterruptedException e) {
            result = "发送失败: " + e.getMessage(); // Include exception message
            // Consider logging the exception properly instead of just wrapping
            throw new RuntimeException(e); // Re-throw as RuntimeException as in original code
        } catch (Exception e) { // Catch other potential exceptions during setup or write
            result = "发送失败: " + e.getMessage();
            throw new RuntimeException(e);
        } finally {
            // Close the channel if it was successfully opened
            if (channel != null) {
                try {
                    channel.close().sync(); // Close the channel and wait
                } catch (InterruptedException e) {
                    // Log this exception if needed
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
            // Shutdown the group
            group.shutdownGracefully();
        }
        return result;
    }

    private ByteBuf buildWolPacket(String macAddress) {
        // 将MAC地址转换为字节数组
        byte[] macBytes = parseMacAddress(macAddress);

        // 构建魔术包: 6x0xFF + 16*MAC
        ByteBuf buffer = Unpooled.buffer(6 + 16 * macBytes.length);
        for (int i = 0; i < 6; i++) {
            buffer.writeByte(0xFF);
        }
        for (int i = 0; i < 16; i++) {
            buffer.writeBytes(macBytes);
        }
        return buffer;
    }

    private byte[] parseMacAddress(String macAddress) {
        // 处理MAC地址格式（支持XX:XX:XX:XX:XX:XX或XX-XX-XX-XX-XX-XX）
        String[] hex = macAddress.split("[:\\-]");
        if (hex.length != 6) {
            throw new IllegalArgumentException("无效的MAC地址格式" + macAddress);
        }

        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) Integer.parseInt(hex[i], 16);
        }
        return bytes;
    }

}
