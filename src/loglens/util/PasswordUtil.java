package loglens.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Utility class for cryptographic password hashing using SHA-256.
//
// Why do we store a password hash instead of the plaintext password?
// Storing plain passwords in a database is a major security vulnerability. If the database
// is breached, leaked, or exposed via backups, an attacker instantly gains access to every
// account. A cryptographic one-way hash (like SHA-256) transforms the password into a fixed-length
// irreversible digital fingerprint. When a user logs in, we hash the password they entered and
// compare hashes. If the hashes match, the password is correct, and we never have to store or know
// the actual plaintext password!
public class PasswordUtil {

    private PasswordUtil() {
    }

    // Computes the SHA-256 hash of a string and returns it as a lowercase hex string
    public static String hash(String password) {
        if (password == null) {
            password = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xff & digest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JRE", e);
        }
    }
}
