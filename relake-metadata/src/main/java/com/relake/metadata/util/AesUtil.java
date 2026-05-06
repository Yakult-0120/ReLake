package com.relake.metadata.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 密码加解密工具
 */
@Component
public class AesUtil {

    // AES-128 密钥（16字节 "ReLake16AES!2026"）
    private static final byte[] AES_KEY = "ReLake16AES!2026".getBytes(StandardCharsets.UTF_8);

    private final AES aes;

    public AesUtil() {
        this.aes = SecureUtil.aes(AES_KEY);
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        return aes.encryptBase64(plainText);
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        return aes.decryptStr(cipherText);
    }
}
