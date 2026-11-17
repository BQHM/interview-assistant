package com.interview.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.interview.infrastructure.ai.LlmProviderProperties;

/**
 * 文件功能说明
 * <p>负责验证 API Key 加解密的安全语义。</p>
 *
 * @author NobuNo
 * @date 2026-11-17
 */
class AesGcmCipherTest {

    private static final String PLAINTEXT = "sk-test-api-key-1234567890";

    private static final int NONCE_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BYTES = 16;

    private AesGcmCipher aesGcmCipher;

    /**
     * 功能说明
     * <p>用随机主密钥构造被测对象。</p>
     */
    @BeforeEach
    void setUp() {
        aesGcmCipher = new AesGcmCipher(propertiesWithKey(randomBase64Key()));
    }

    /**
     * 功能说明
     * <p>加密后解密应还原原文。</p>
     */
    @Test
    void encryptThenDecrypt_shouldReturnOriginalPlaintext() {
        AesGcmCipher.CipherPayload payload = aesGcmCipher.encrypt(PLAINTEXT);

        String decrypted = aesGcmCipher.decrypt(payload.ciphertext(), payload.nonce());

        assertThat(decrypted).isEqualTo(PLAINTEXT);
    }

    /**
     * 功能说明
     * <p>同一明文加密两次应得到不同的 nonce 和密文，证明 nonce 每次重新生成。</p>
     */
    @Test
    void encryptTwice_shouldProduceDifferentNonceAndCiphertext() {
        AesGcmCipher.CipherPayload first = aesGcmCipher.encrypt(PLAINTEXT);
        AesGcmCipher.CipherPayload second = aesGcmCipher.encrypt(PLAINTEXT);

        assertThat(first.nonce()).isNotEqualTo(second.nonce());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }

    /**
     * 功能说明
     * <p>nonce 解码后应为 12 字节。</p>
     */
    @Test
    void encrypt_shouldProduceTwelveByteNonce() {
        AesGcmCipher.CipherPayload payload = aesGcmCipher.encrypt(PLAINTEXT);

        byte[] nonceBytes = Base64.getDecoder().decode(payload.nonce());

        assertThat(nonceBytes).hasSize(NONCE_LENGTH_BYTES);
    }

    /**
     * 功能说明
     * <p>密文应比明文字节数多出 16 字节认证标签。</p>
     */
    @Test
    void encrypt_shouldAppendSixteenByteAuthTag() {
        AesGcmCipher.CipherPayload payload = aesGcmCipher.encrypt(PLAINTEXT);

        byte[] cipherBytes = Base64.getDecoder().decode(payload.ciphertext());
        int plainBytesLength = PLAINTEXT.getBytes(StandardCharsets.UTF_8).length;

        assertThat(cipherBytes).hasSize(plainBytesLength + GCM_TAG_LENGTH_BYTES);
    }

    /**
     * 功能说明
     * <p>换用不同主密钥解密应失败，且提示认证校验未通过。</p>
     */
    @Test
    void decryptWithDifferentKey_shouldFailWithAuthTagMessage() {
        AesGcmCipher.CipherPayload payload = aesGcmCipher.encrypt(PLAINTEXT);
        AesGcmCipher anotherCipher = new AesGcmCipher(propertiesWithKey(randomBase64Key()));

        assertThatThrownBy(() -> anotherCipher.decrypt(payload.ciphertext(), payload.nonce()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("认证校验未通过");
    }

    /**
     * 功能说明
     * <p>密文被篡改应解密失败，而不是解出错误明文。</p>
     */
    @Test
    void decryptTamperedCiphertext_shouldFail() {
        AesGcmCipher.CipherPayload payload = aesGcmCipher.encrypt(PLAINTEXT);
        byte[] cipherBytes = Base64.getDecoder().decode(payload.ciphertext());
        cipherBytes[cipherBytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(cipherBytes);

        assertThatThrownBy(() -> aesGcmCipher.decrypt(tampered, payload.nonce()))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 功能说明
     * <p>加密空明文应被拒绝。</p>
     */
    @Test
    void encrypt_shouldRejectEmptyPlaintext() {
        assertThatThrownBy(() -> aesGcmCipher.encrypt(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    /**
     * 功能说明
     * <p>未配置主密钥时应拒绝构造。</p>
     */
    @Test
    void constructor_shouldRejectMissingKey() {
        LlmProviderProperties properties = new LlmProviderProperties();
        properties.setEncryptionKey("");

        assertThatThrownBy(() -> new AesGcmCipher(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_LLM_ENCRYPTION_KEY");
    }

    /**
     * 功能说明
     * <p>主密钥长度不是 32 字节时应拒绝构造。</p>
     */
    @Test
    void constructor_shouldRejectWrongKeyLength() {
        LlmProviderProperties properties = new LlmProviderProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[3]));

        assertThatThrownBy(() -> new AesGcmCipher(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }

    /**
     * 功能说明
     * <p>构造带指定主密钥的配置对象。</p>
     *
     * @param base64Key Base64 编码的主密钥
     * @return LLM Provider 配置
     * @author NobuNo
     * @date 2026-11-17
     */
    private LlmProviderProperties propertiesWithKey(String base64Key) {
        LlmProviderProperties properties = new LlmProviderProperties();
        properties.setEncryptionKey(base64Key);
        return properties;
    }

    /**
     * 功能说明
     * <p>生成 32 字节随机主密钥并转成 Base64。</p>
     *
     * @return Base64 编码的主密钥
     * @author NobuNo
     * @date 2026-11-17
     */
    private String randomBase64Key() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
