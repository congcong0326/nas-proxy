//package org.congcong.nasproxy.core.monitor.web;
//
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.handler.codec.http.HttpObjectAggregator;
//import io.netty.handler.codec.http.HttpServerCodec;
//
//public class MonitorAdminWebInitializer extends ChannelInitializer<SocketChannel> {
//    @Override
//    protected void initChannel(SocketChannel socketChannel) throws Exception {
//        socketChannel.pipeline().addLast(
//                new HttpServerCodec(),
//                new HttpObjectAggregator(65536),
//                new TrafficAnalysisHandler()
//        );
//    }
//}
