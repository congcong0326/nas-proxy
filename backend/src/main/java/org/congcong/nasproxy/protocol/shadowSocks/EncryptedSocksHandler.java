package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.util.ByteBufSplitter;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm.CryptoProcessor;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm.HKDF;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm.NonceUtil;

import java.security.SecureRandom;
import java.util.List;

/**
 * 加密出站数据
 */
@Slf4j
public class EncryptedSocksHandler extends MessageToByteEncoder<ByteBuf> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private boolean firstPacket = true;
    private long encryptCounter = 0;

    private final CryptoProcessor cryptoProcessor;

    public EncryptedSocksHandler(CryptoProcessor cryptoProcessor) {
        this.cryptoProcessor = cryptoProcessor;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (msg.readableBytes() == 0) {
            return;
        }
        // 第一个包需要发送salt
        if (firstPacket) {
            byte[] salt = new byte[cryptoProcessor.getSaltSize()];
            SECURE_RANDOM.nextBytes(salt);
            out.writeBytes(salt);

            // 生成子密钥
            byte[] subkey = HKDF.deriveKey(cryptoProcessor.getKey(), salt, cryptoProcessor.getKeySize());
            cryptoProcessor.refreshKey(subkey);
            firstPacket = false;
        }
        List<ByteBuf> chunks = ByteBufSplitter.splitByteBuf(msg);
        for (ByteBuf chunk : chunks) {
            writeChunk(chunk, out);
        }
    }

    private void writeChunk(ByteBuf msg, ByteBuf out) throws Exception {
        int payloadLength = msg.readableBytes();
        //log.info("send {} bytes", payloadLength);
        // 加密长度字段
        byte[] lengthBytes = new byte[2];
        lengthBytes[0] = (byte)(payloadLength >>> 8);
        lengthBytes[1] = (byte)payloadLength;
        byte[] encryptedLength = cryptoProcessor.encrypt(lengthBytes, NonceUtil.generateNonce(encryptCounter ++));
        out.writeBytes(encryptedLength);
        // 加密数据
        byte[] payload = new byte[payloadLength];
        msg.readBytes(payload);
        byte[] encryptedPayload = cryptoProcessor.encrypt(payload, NonceUtil.generateNonce(encryptCounter ++));
        out.writeBytes(encryptedPayload);
    }



}
