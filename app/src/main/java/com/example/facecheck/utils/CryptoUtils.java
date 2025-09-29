package com.example.facecheck.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String TAG = "CryptoUtils";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static String generateRandomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.encodeToString(key, Base64.NO_WRAP);
    }

    public static String encrypt(String plaintext, String keyAlias) {
        try {
            // 获取密钥
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            SecretKey secretKey;
            if (!keyStore.containsAlias(keyAlias)) {
                // 生成新密钥
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
                keyGenerator.init(keyGenParameterSpec);
                secretKey = keyGenerator.generateKey();
            } else {
                // 使用已有密钥
                secretKey = (SecretKey) keyStore.getKey(keyAlias, null);
            }

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();

            // 组合 IV 和密文
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption error", e);
            return null;
        }
    }

    public static String decrypt(String ciphertext, String keyAlias) {
        try {
            // 获取密钥
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyAlias, null);

            // 解码并分离 IV 和密文
            byte[] decoded = Base64.decode(ciphertext, Base64.NO_WRAP);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 解密
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption error", e);
            return null;
        }
    }

    public static String encryptWithPassword(String plaintext, String password) {
        try {
            // 生成随机盐
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            // 使用密码和盐生成密钥
            SecretKey key = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "AES");

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();

            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合盐、IV 和密文
            byte[] combined = new byte[salt.length + GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, salt.length + GCM_IV_LENGTH, ciphertext.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption with password error", e);
            return null;
        }
    }

    public static String decryptWithPassword(String ciphertext, String password) {
        try {
            // 解码并分离盐、IV 和密文
            byte[] decoded = Base64.decode(ciphertext, Base64.NO_WRAP);
            byte[] salt = new byte[16];
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - salt.length - GCM_IV_LENGTH];
            
            System.arraycopy(decoded, 0, salt, 0, salt.length);
            System.arraycopy(decoded, salt.length, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, salt.length + GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // 使用密码和盐生成密钥
            SecretKey key = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "AES");

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // 解密
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption with password error", e);
            return null;
        }
    }
}