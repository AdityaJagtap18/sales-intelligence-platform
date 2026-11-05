package com.nexora.platform.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 (JDK built-in {@code javax.crypto}) password hashing. This
 * offline Maven build has no Spring Security or a dedicated bcrypt library
 * cached, so this uses the strongest adaptive KDF available in the JDK
 * standard library instead of a third-party dependency or (worse) a plain
 * unsalted digest — see docs/implementation/known-limitations.md.
 */
final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String hash(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable in this JVM", e);
        }
    }

    static boolean matches(String password, String saltBase64, String expectedHash) {
        String actual = hash(password, saltBase64);
        return MessageDigest.isEqual(actual.getBytes(), expectedHash.getBytes());
    }
}
