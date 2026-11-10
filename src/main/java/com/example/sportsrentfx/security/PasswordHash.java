package com.example.sportsrentfx.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Console;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHash {

    private static final int ITERATIONS = 600_000;

    private PasswordHash() {}

    public static String create(char[] password) {
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException(
                    "Password must contain 12–128 characters."
            );
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        return "pbkdf2-sha256$"
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(
                derive(password, salt, ITERATIONS)
        );
    }

    public static boolean verify(char[] password, String storedHash) {
        if (password == null || password.length > 128 || storedHash == null) {
            return false;
        }

        byte[] actual = null;

        try {
            String[] parts = storedHash.split("\\$");

            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);

            if (iterations < ITERATIONS || iterations > 2_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            if (salt.length != 16 || expected.length != 32) {
                return false;
            }

            actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);

        } catch (IllegalArgumentException exception) {
            return false;
        } finally {
            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }
        }
    }

    private static byte[] derive(
            char[] password,
            byte[] salt,
            int iterations
    ) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);

        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Password verification unavailable.", exception
            );
        } finally {
            spec.clearPassword();
        }
    }

    // Run this file from a terminal to generate your administrator password hash.
    public static void main(String[] args) {
        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException("Run this from a terminal.");
        }

        char[] password = console.readPassword("New admin password: ");
        char[] confirmation = console.readPassword("Confirm password: ");

        try {
            if (password == null || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            System.out.println(create(password));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}