package org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm;


public class ChaCha20Poly1305Processor implements CryptoProcessor {
    private byte[] secretKey;

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] nonce) throws Exception {
        return ChaCha20Poly1305ByJava11.encrypt(plaintext, getKey(), nonce);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] nonce) throws Exception {
        // jdk11原生支持该算法
        return ChaCha20Poly1305ByJava11.decrypt(ciphertext, getKey(), nonce);
    }


    @Override
    public byte[] getKey() {
        return secretKey;
    }

    @Override
    public void refreshKey(byte[] key) {
        secretKey = key;
    }

    @Override
    public void setKey(byte[] key) {
        this.secretKey = key;
    }

    @Override
    public int getKeySize() {
        return 32;
    }

    @Override
    public int getSaltSize() {
        return 32;
    }

    @Override
    public int getNonceSize() {
        return 12;
    }

    @Override
    public int getTagSize() {
        return 16;
    }
}
