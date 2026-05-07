package com.adjuge.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Utility class for password hashing and verification.
 *
 * Uses PBKDF2WithHmacSHA256 — a key-derivation function designed
 * specifically for password hashing. It is intentionally slow to
 * make brute-force attacks expensive.
 *
 * Why not MD5 / SHA-1?
 *  - Those are fast hashing algorithms — great for checksums, terrible for passwords.
 *  - A modern GPU can compute billions of MD5 hashes per second.
 *  - PBKDF2 adds a "work factor" (iterations) that makes each attempt slow.
 *
 * Why a Salt?
 *  - Without a salt, two users with the same password produce the same hash.
 *  - An attacker could pre-compute a "rainbow table" and look up any hash instantly.
 *  - A random salt makes every hash unique, defeating rainbow tables entirely.
 *
 * TODO(@nhan): Còn phải thêm phần migrate data cũ trong DataStore.seedData()
 * TODO(@nhan): Cần test lại edge case password rỗng và password có ký tự đặc biệt
 */
public class PasswordUtil {

    // PBKDF2 configuration constants
    private static final String ALGORITHM    = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS   = 310_000;   // NIST SP 800-132 recommendation (2023)
    private static final int    KEY_LENGTH   = 256;       // bits → 32 bytes output
    private static final int    SALT_LENGTH  = 16;        // bytes → 128-bit salt

    // Separator used in the stored hash string: "iterations:saltHex:hashHex"
    private static final String SEPARATOR = ":";

    /**
     * Hashes a plain-text password securely.
     *
     * The returned string encodes all information needed to verify the password later:
     *   "<iterations>:<saltHex>:<hashHex>"
     * Example: "310000:4a2f...8c:9b3e...1d"
     *
     * @param plainText the raw password entered by the user
     * @return a self-contained hash string safe to store in the database
     * @throws RuntimeException if the JVM is missing required crypto support (should never happen)
     */
    public static String hashPassword(String plainText) {
        try {
            // Step 1: Generate a cryptographically-random 16-byte salt
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            // Step 2: Derive the key using PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                    plainText.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword(); // Clear sensitive data from memory immediately

            // Step 3: Encode as a self-describing string
            return ITERATIONS + SEPARATOR + toHex(salt) + SEPARATOR + toHex(hash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Should never happen on any standard JVM — all required algorithms are mandatory
            throw new RuntimeException("[PasswordUtil] Crypto error — JVM is missing PBKDF2 support", e);
        }
    }

    /**
     * Verifies a plain-text password against a previously hashed string.
     *
     * Re-hashes the candidate password with the same salt and iterations,
     * then compares using a constant-time equality check to prevent timing attacks.
     *
     * @param plainText  the raw password to verify
     * @param storedHash the hash string produced by {@link #hashPassword(String)}
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainText, String storedHash) {
        try {
            // Step 1: Parse the stored hash string
            String[] parts = storedHash.split(SEPARATOR);
            if (parts.length != 3) {
                return false; // Malformed hash — treat as mismatch
            }
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt    = fromHex(parts[1]);
            byte[] expected = fromHex(parts[2]);

            // Step 2: Re-hash the candidate password with the SAME salt + iterations
            PBEKeySpec spec = new PBEKeySpec(
                    plainText.toCharArray(), salt, iterations, expected.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] actual = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();

            // Step 3: Constant-time comparison — prevents timing attacks
            // (regular .equals() would return early on first mismatch, leaking info)
            return MessageDigest.isEqual(expected, actual);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NumberFormatException e) {
            return false;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Converts a byte array to a lowercase hex string.
     * Example: {0x4A, 0xFF} → "4aff"
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts a hex string back to a byte array.
     * Example: "4aff" → {0x4A, 0xFF}
     */
    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
