package org.tinycloud.jdbc.id;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A class that generates ULIDs.
 * <p>
 *  Both types of ULID can be easily created by this generator, i.e. monotonic
 *  and non-monotonic.
 * <p>
 *     <pre>
 *         Ulid ulid = UlidCreator.getUlid();
 *         Ulid ulid = UlidCreator.getMonotonicUlid();
 *
 *         System.out.println(ulid.toString());
 *         </per>
 * In addition, a "non-standard" hash-based ULID can also be generated, in which
 * the random component is replaced with the first 10 bytes of an SHA-256 hash.
 */
public final class UlidCreator {

    private UlidCreator() {
    }

    /**
     * Returns a ULID.
     * <p>
     * The random component is reset for each new ULID generated.
     *
     * @return a ULID
     */
    public static Ulid getUlid() {
        return UlidFactoryHolder.INSTANCE.create();
    }

    /**
     * Returns a ULID.
     * <p>
     * The random component is reset for each new ULID generated.
     *
     * @param time the current time in milliseconds, measured from the UNIX epoch of
     *             1970-01-01T00:00Z (UTC)
     * @return a ULID
     */
    public static Ulid getUlid(final long time) {
        return UlidFactoryHolder.INSTANCE.create(time);
    }

    /**
     * Returns a Monotonic ULID.
     * <p>
     * The random component is incremented for each new ULID generated in the same
     * millisecond.
     *
     * @return a ULID
     */
    public static Ulid getMonotonicUlid() {
        return MonotonicFactoryHolder.INSTANCE.create();
    }

    /**
     * Returns a Monotonic ULID.
     * <p>
     * The random component is incremented for each new ULID generated in the same
     * millisecond.
     *
     * @param time the current time in milliseconds, measured from the UNIX epoch of
     *             1970-01-01T00:00Z (UTC)
     * @return a ULID
     */
    public static Ulid getMonotonicUlid(final long time) {
        return MonotonicFactoryHolder.INSTANCE.create(time);
    }

    /**
     * Returns a Hash ULID.
     * <p>
     * The random component is replaced with the first 10 bytes of an SHA-256 hash.
     * <p>
     * It always returns the same ULID for a specific pair of {@code time} and
     * {@code string}.
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * long time = file.getCreatedAt();
     * String name = file.getFileName();
     * Ulid ulid = UlidCreator.getHashUlid(time, name);
     * }</pre>
     *
     * @param time   the time in milliseconds, measured from the UNIX epoch of
     *               1970-01-01T00:00Z (UTC)
     * @param string a string to be hashed using SHA-256 algorithm.
     * @return a ULID
     * @since 5.2.0
     */
    public static Ulid getHashUlid(final long time, String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return getHashUlid(time, bytes);
    }

    /**
     * Returns a Hash ULID.
     * <p>
     * The random component is replaced with the first 10 bytes of an SHA-256 hash.
     * <p>
     * It always returns the same ULID for a specific pair of {@code time} and
     * {@code bytes}.
     * <p>
     * Usage example:
     *
     * <pre>{@code
     * long time = file.getCreatedAt();
     * byte[] bytes = file.getFileBinary();
     * Ulid ulid = UlidCreator.getHashUlid(time, bytes);
     * }</pre>
     *
     * @param time  the time in milliseconds, measured from the UNIX epoch of
     *              1970-01-01T00:00Z (UTC)
     * @param bytes a byte array to be hashed using SHA-256 algorithm.
     * @return a ULID
     * @since 5.2.0
     */
    public static Ulid getHashUlid(final long time, byte[] bytes) {
        // Calculate the hash and take the first 10 bytes
        byte[] hash = hasher("SHA-256").digest(bytes);
        byte[] rand = Arrays.copyOf(hash, 10);
        return new Ulid(time, rand);
    }

    private static MessageDigest hasher(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("%s not supported", algorithm));
        }
    }

    private static class UlidFactoryHolder {
        static final UlidFactory INSTANCE = UlidFactory.newInstance();
    }

    private static class MonotonicFactoryHolder {
        static final UlidFactory INSTANCE = UlidFactory.newMonotonicInstance();
    }
}
