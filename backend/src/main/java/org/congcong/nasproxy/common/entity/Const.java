package org.congcong.nasproxy.common.entity;

import io.netty.util.AttributeKey;
import org.congcong.nasproxy.protocol.shadowSocks.ReplayAttackCheck;

public class Const {


    public static final AttributeKey<Context> CONTEXT = AttributeKey.valueOf("my_context");

    public static final AttributeKey<ReplayAttackCheck> ATTACK_LOG = AttributeKey.valueOf("my_attack_check");

    public static final String METHOD = "method";

    public static final String PASSWORD = "password";


    // 密码解密错误，这种有可能回报错，此时应该保持沉默
    public static final int AUTH_FAILED = 0;

    // 缺少预期的字节数，可能导致服务器一直等待，所以应对这种应该主动写回数据
    public static final int BYTE_SALT_ABSENT = 1;

    public static final int BYTE_LENGTH_ABSENT = 2;

    public static final int BYTE_PAYLOAD_ABSENT = 3;

    // 非法的盐值，服务器可能正常代理，此时应该保持沉默
    public static final int ILLEGAL_SALT = 4;

    public static String translateAttackType(int attackType) {
        switch (attackType) {
            case AUTH_FAILED: return "认证失败，不返回数据，维持连接";
            case BYTE_SALT_ABSENT: return "请求缺少salt，可能导致服务器一直等待，返回数据";
            case BYTE_LENGTH_ABSENT: return "请求缺少长度字节，可能导致服务器一直等待，返回数据";
            case BYTE_PAYLOAD_ABSENT: return "请求缺少负载字节，可能导致服务器一直等待，返回数据";
            case ILLEGAL_SALT: return "非法的盐，不返回数据，维持连接";
            default: return "未知类型";
        }
    }

}
