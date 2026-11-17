package com.interview.infrastructure.crypto;

import com.interview.infrastructure.ai.LlmProviderProperties;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 文件功能说明
 * <p>负责 API Key 的 AES-GCM 加解密。</p>
 *
 * @author NobuNo
 * @date 2026-11-17
 */
@Component
public class AesGcmCipher {

    // 加密算法与模式：AES + GCM，无填充（GCM 自己处理）
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    // 认证标签长度，GCM 固定 128 位
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // nonce 长度 12 字节，是 AES-GCM 的推荐值
    private static final int NONCE_LENGTH_BYTES = 12;

    // AES-256 要求密钥 32 字节
    private static final int KEY_LENGTH_BYTES = 32;

    // SecureRandom 线程安全，可以复用
    private final SecureRandom secureRandom = new SecureRandom();

    private final SecretKey secretKey;

    /**
     * 功能说明
     * <p>校验并加载 API Key 加密主密钥，配置非法时直接终止启动。</p>
     *
     * @param properties LLM Provider 配置
     * @author NobuNo
     * @date 2026-11-17
     */
    public AesGcmCipher(LlmProviderProperties properties) {
        // 要点：密钥缺失或长度不对，直接抛 IllegalStateException，让应用起不来
        String base64Key = properties.getEncryptionKey();

        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("未配置 API Key 加密主密钥，请设置环境变量 AI_LLM_ENCRYPTION_KEY");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("加密主密钥不是合法的 Base64 字符串", e);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException("加密主密钥长度必须是 32 字节（AES-256），当前为 " + keyBytes.length + " 字节");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 功能说明
     * <p>加密明文，返回密文与本次使用的 nonce。</p>
     *
     * @param plaintext 待加密明文
     * @return 密文与 nonce
     * @throws IllegalArgumentException 当明文为空时抛出
     * @throws IllegalStateException    当底层加密失败时抛出
     * @author NobuNo
     * @date 2026-11-17
     */
    public CipherPayload encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("待加密内容不能为空");
        }

        try {
            // ① 每次加密都生成新的 12 字节 nonce
            byte[] nonceBytes = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonceBytes);

            // ② Cipher 不是线程安全的，每次加解密都新建一个
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // ③ 初始化：加密模式 + 密钥 + nonce 与标签长度
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonceBytes));

            // ④ 执行加密：结果 = 密文 + 末尾 16 字节认证标签
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // ⑤ 二进制不能直接存库，Base64 转成文本，密文和 nonce 都要存
            Base64.Encoder encoder = Base64.getEncoder();
            return new CipherPayload(encoder.encodeToString(cipherBytes), encoder.encodeToString(nonceBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    /**
     * 功能说明
     * <p>用密文和 nonce 解密出明文。</p>
     *
     * @param ciphertext Base64 密文
     * @param nonce      Base64 nonce
     * @return 明文
     * @throws IllegalArgumentException 当密文或 nonce 为空时抛出
     * @throws IllegalStateException    当解密失败、认证校验未通过或数据非法时抛出
     * @author NobuNo
     * @date 2026-11-17
     */
    public String decrypt(String ciphertext, String nonce) {
        if (ciphertext == null || ciphertext.isEmpty() || nonce == null || nonce.isEmpty()) {
            throw new IllegalArgumentException("密文与 nonce 不能为空");
        }

        try {
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext.trim());
            byte[] nonceBytes = Base64.getDecoder().decode(nonce.trim());

            if (nonceBytes.length != NONCE_LENGTH_BYTES) {
                throw new IllegalStateException("nonce 长度非法，应为 12 字节，实际为 " + nonceBytes.length + " 字节");
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonceBytes));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new IllegalStateException("API Key 解密失败：认证校验未通过，加密主密钥可能已更换或密文被篡改", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("API Key 解密失败：密文或 nonce 不是合法的 Base64 字符串", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }

    /**
     * 加解密结果，密文与 nonce 必须成对保存。
     */
    public record CipherPayload(String ciphertext, String nonce) {
    }
}