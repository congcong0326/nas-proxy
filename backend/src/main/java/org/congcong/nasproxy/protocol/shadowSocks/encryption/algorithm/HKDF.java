package org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HKDF {

    public static byte[] kdf(String password, int keyLen) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] b = new byte[0];
            byte[] prev = new byte[0];

            while (b.length < keyLen) {
                md5.update(prev);
                md5.update(password.getBytes());
                byte[] hash = md5.digest();

                // Expand b array to accommodate new hash bytes
                byte[] newB = new byte[b.length + hash.length];
                System.arraycopy(b, 0, newB, 0, b.length);
                System.arraycopy(hash, 0, newB, b.length, hash.length);
                b = newB;

                // Update prev to the latest hash
                prev = Arrays.copyOfRange(b, b.length - md5.getDigestLength(), b.length);

                md5.reset();
            }

            return Arrays.copyOf(b, keyLen);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    public static byte[] deriveKey(byte[] masterKey, byte[] salt, int length) throws Exception {
        if (length <= 0) {
            throw new IllegalArgumentException("Key length must be a positive integer.");
        }

        // 初始化 HKDF-SHA1 派生器
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA1Digest());
        HKDFParameters params = new HKDFParameters(masterKey, salt, "ss-subkey".getBytes());

        // 创建用于存放子密钥的缓冲区
        byte[] subkey = new byte[length];
        hkdf.init(params);

        // 生成指定长度的子密钥
        hkdf.generateBytes(subkey, 0, length);
        return subkey;
    }
}
