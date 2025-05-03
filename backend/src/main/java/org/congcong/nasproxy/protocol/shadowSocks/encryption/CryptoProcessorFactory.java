package org.congcong.nasproxy.protocol.shadowSocks.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm.*;

import java.security.Security;

public class CryptoProcessorFactory {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }



    public static CryptoProcessor createProcessor(Algorithm algorithm, String key) {
        CryptoProcessor cryptoProcessor = null;
        switch (algorithm) {
            case aes_256_gcm:
                cryptoProcessor = new AESGCMProcessor();
                break;
            case aes_128_gcm:
                cryptoProcessor = new AES128GCMProcessor();
                break;
            case chacha20_poly1305:
                cryptoProcessor = new ChaCha20Poly1305Processor();
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        cryptoProcessor.setKey(HKDF.kdf(key, cryptoProcessor.getKeySize()));
        return cryptoProcessor;
    }

}
