package com.sentinelgate.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryptor/decryptor component for Spring Boot.
 *
 * Usage:
 *  - Provide a Base64-encoded AES key (128/192/256 bits) via configuration (preferred)
 *  - Or generate a key using generateBase64Key()
 *
 * The encrypted payload format returned by encrypt(...) is:
 *   Base64( IV(12 bytes) || ciphertext || authTag )
 *
 * This class is intentionally simple and focused on correct usage of AES-GCM.
 */
@Component
public class EncryptionUtils {

    // GCM recommended IV length = 12 bytes (96 bits)
    private static final int IV_LENGTH_BYTES = 12;
    // GCM auth tag length in bits (128 is recommended)
    private static final int TAG_LENGTH_BITS = 128;
    private static final String AES_ALGO = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Provide key as Base64 in properties or environment variable.
     * Example (application.properties):
     *   security.aes.base64-key=BASE64_ENCODED_KEY_HERE
     *
     * If not provided, this component will remain usable by calling generateKey(...) manually.
     */
    @Value("${encryption.password-key:null}")
    private String accessKey;

    // cached SecretKey if provided via config
    private SecretKey key;

    @PostConstruct
    public void init() {
        if (accessKey != null && !accessKey.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(accessKey);
            key = new SecretKeySpec(keyBytes, AES_ALGO);
        }
    }

    /**
     * Encrypts plaintext using AES-GCM with the provided secret key.
     * Returns Base64 encoded payload containing IV || ciphertext || authTag.
     *
     * @param plainText plaintext to encrypt
     * @return Base64 encoded (iv || ciphertext || tag)
     */
    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to the ciphertext so we can extract on decrypt
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        byte[] cipherMessage = byteBuffer.array();

        return Base64.getEncoder().encodeToString(cipherMessage);
    }

    /**
     * Decrypts a Base64 encoded payload created by encrypt(...) above.
     *
     * @param base64CipherMessage Base64(iv || ciphertext || tag)
     * @return decrypted plaintext
     */
    public String decrypt(String base64CipherMessage) throws Exception {
        byte[] cipherMessage = Base64.getDecoder().decode(base64CipherMessage);

        if (cipherMessage.length < IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid cipher text");
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(cipherMessage, 0, iv, 0, iv.length);

        byte[] cipherText = new byte[cipherMessage.length - iv.length];
        System.arraycopy(cipherMessage, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plainBytes = cipher.doFinal(cipherText);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
