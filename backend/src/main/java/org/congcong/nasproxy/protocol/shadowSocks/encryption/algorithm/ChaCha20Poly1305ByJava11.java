package org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * java 11 support
 */
public class ChaCha20Poly1305ByJava11 {

    public static byte[] encrypt(byte[] data,byte[] key, byte[] nonceBytes) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if(key == null) throw new InvalidKeyException("SecretKey must NOT be NULL");

        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");

        // Create IvParamterSpec
        AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(nonceBytes);

        // Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key, "ChaCha20");

        // Initialize Cipher for ENCRYPT_MODE
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

        // Perform Encryption
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] cipherText, byte[] key, byte[] nonceBytes) throws Exception {

        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");

        // Create IvParamterSpec
        AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(nonceBytes);

        // Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key, "ChaCha20");

        // Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

        // Perform Decryption
        return cipher.doFinal(cipherText);
    }

}
