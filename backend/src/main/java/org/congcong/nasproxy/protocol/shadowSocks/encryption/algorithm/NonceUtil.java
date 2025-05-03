package org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm;

public class NonceUtil {

    public static byte[] generateNonce(long counter) {
        byte[] nonce = new byte[12];
        for (int i = 0; i < 8; i++) {
            nonce[i] = (byte)(counter >>> (i * 8));
        }
        return nonce;
    }

}
