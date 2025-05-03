package org.congcong.nasproxy.core.monitor.handler;

import io.netty.channel.ChannelHandlerContext;
import org.congcong.nasproxy.common.entity.Const;
import org.congcong.nasproxy.common.entity.Context;
import org.congcong.nasproxy.core.monitor.UserTrafficType;


public class TrafficHandlerUtil {

    public static void handleTraffic(ChannelHandlerContext ctx, int bytes, UserTrafficType trafficType) {
        Context context = ctx.channel().attr(Const.CONTEXT).get();
        if (context != null) {
            if (trafficType == UserTrafficType.UP) {
                context.setUp(context.getUp() + bytes);
            } else {
                context.setDown(context.getDown() + bytes);
            }
        }
    }
}
