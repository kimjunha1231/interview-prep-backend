package com.junha.interview.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EncryptionUtilsTest {

    private EncryptionUtils encryptionUtils;

    @BeforeEach
    void setUp() {
        // Use a test key
        encryptionUtils = new EncryptionUtils("mySuperSecretTestKey123456789012");
    }

    @Test
    @DisplayName("이메일 주소를 SHA-256 해싱하면 항상 동일한 64글자의 16진수 해시값이 생성된다")
    void testHashEmail() {
        String email = "test@example.com";
        String hash1 = encryptionUtils.hash(email);
        String hash2 = encryptionUtils.hash(email);

        assertThat(hash1).isNotNull();
        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("이메일 주소를 AES-256 암호화 후 다시 복호화하면 원본 주소로 동일하게 복원된다")
    void testEncryptDecryptEmail() {
        String email = "user@gmail.com";
        String encrypted = encryptionUtils.encrypt(email);
        String decrypted = encryptionUtils.decrypt(encrypted);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(email);
        assertThat(decrypted).isEqualTo(email);
    }
}
