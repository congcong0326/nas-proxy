package org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm;

public interface CryptoProcessor {
    byte[] encrypt(byte[] plaintext, byte[] nonce) throws Exception;
    byte[] decrypt(byte[] ciphertext, byte[] nonce) throws Exception;

    byte[] getKey();

    void refreshKey(byte[] key);

    void setKey(byte[] key);

    int getKeySize();

    int getSaltSize();

    int getNonceSize();

    int getTagSize();
}
