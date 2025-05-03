package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.congcong.nasproxy.common.entity.AttackLog;
import org.congcong.nasproxy.common.entity.Const;
import org.congcong.nasproxy.core.monitor.connect.ConnectionMonitor;
import org.congcong.nasproxy.core.monitor.connect.IConnectionReport;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ReplayAttackCheck    {

    private static final IConnectionReport connectionReport = ConnectionMonitor.getInstance();

    @Getter
    private volatile boolean isAttack = false;

    @Getter
    private final AtomicBoolean cancelConnect = new AtomicBoolean(true);

    private ChannelHandlerContext channelHandlerContext;

    private AttackLog attackLog;

    private boolean firstSubmit = false;


    private ReplayAttackCheck() {

    }

    public static ReplayAttackCheck get(ChannelHandlerContext ctx) {
        return ctx.channel().attr(Const.ATTACK_LOG).get();
    }

    public ReplayAttackCheck setType(int type) {
        attackLog.setAttackType(type);
        return this;
    }

    public ReplayAttackCheck setReceiveByte(int receiveByte) {
        attackLog.setReceiveBytes(receiveByte);
        return this;
    }

    public ReplayAttackCheck addReceiveByte(int receiveByte) {
        attackLog.setReceiveBytes(attackLog.getReceiveBytes() +receiveByte);
        return this;
    }

    public static ReplayAttackCheck init(ChannelHandlerContext ctx) {
        if (ctx.channel().attr(Const.ATTACK_LOG).get() != null) {
            return ctx.channel().attr(Const.ATTACK_LOG).get();
        }
        ReplayAttackCheck replayAttackCheck = new ReplayAttackCheck();
        AttackLog attackLog = new AttackLog();
        attackLog.setStartTime(System.currentTimeMillis());
        int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        attackLog.setPort(port);
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress(); // 获取客户端IP
        attackLog.setIp(clientIp);

        ctx.channel().attr(Const.ATTACK_LOG).set(replayAttackCheck);
        //ctx.channel().pipeline().addFirst(replayAttackCheck);
        replayAttackCheck.channelHandlerContext = ctx;
        replayAttackCheck.attackLog = attackLog;
        return replayAttackCheck;
    }

    public ReplayAttackCheck setAttack(boolean isAttack) {
        this.isAttack = isAttack;
        return this;
    }


    public void handleReplayAttackDelay() {
        if (firstSubmit) {
            firstSubmit = false;
            channelHandlerContext.channel().eventLoop().schedule(() -> writeAndSetDeny(channelHandlerContext), 5, TimeUnit.SECONDS);
        }
    }

    public void handleReplayAttackImmediately() {
        write(channelHandlerContext);
    }

    private void writeAndSetDeny(ChannelHandlerContext ctx) {
        if (cancelConnect.compareAndSet(true, false)) {
            isAttack = true;
            log.warn("delay sending data to take effect, set as an attack");
            write(ctx);
        } else {
            log.debug("delay send cancel");
        }
    }

    private void write(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            if (this.attackLog.getAttackType() == Const.AUTH_FAILED
                    || this.attackLog.getAttackType() == Const.ILLEGAL_SALT) {
                log.warn("silence forever");
            } else {
                log.warn("response forever");
                int length = ThreadLocalRandom.current().nextInt(10, 500);
                attackLog.setSendBytes(attackLog.getSendBytes() + length);
                byte[] randomBytes = new byte[length];
                ThreadLocalRandom.current().nextBytes(randomBytes);
                ByteBuf response = ctx.alloc().buffer(randomBytes.length);
                response.writeBytes(randomBytes);
                ctx.writeAndFlush(response);
            }
        }
    }


    public void fireChannelInactive() throws Exception {
        if (isAttack) {
            attackLog.setLastTime(System.currentTimeMillis());
            connectionReport.reportAttackLog(attackLog);
        }
    }

}
