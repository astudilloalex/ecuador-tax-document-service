package com.alexastudillo.taxdocument.application.invoicedraft;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Versioned, domain-separated privacy-minimal SHA-256 hashing. */
@NullMarked
public final class IdempotencyFingerprint {
  public static final short NORMALIZATION_VERSION = 1;
  private static final byte[] KEY_DOMAIN =
      Objects.requireNonNull("invoice-draft:key:v1\0".getBytes(StandardCharsets.UTF_8));
  private static final byte[] REQUEST_DOMAIN =
      Objects.requireNonNull("invoice-draft:request:v1\0".getBytes(StandardCharsets.UTF_8));

  public byte[] keyHash(String normalizedKey) {
    Objects.requireNonNull(normalizedKey, "normalizedKey");
    MessageDigest digest = sha256();
    digest.update(KEY_DOMAIN);
    put(digest, normalizedKey);
    return Objects.requireNonNull(digest.digest());
  }

  public byte[] requestFingerprint(CreateInvoiceDraftCommand command) {
    Objects.requireNonNull(command, "command");
    MessageDigest digest = sha256();
    digest.update(REQUEST_DOMAIN);
    put(digest, command.emissionPointId());
    put(digest, Objects.requireNonNull(command.emissionDate().toString()));
    put(digest, command.buyer().identificationType());
    put(digest, command.buyer().identification());
    put(digest, command.buyer().legalName());
    putNullable(digest, command.buyer().address());
    putNullable(digest, command.buyer().email());
    putNullable(digest, command.buyer().telephone());
    putCollection(
        digest,
        Objects.requireNonNull(
            command.lines().stream().map(value -> value.toString()).toList(), "line values"),
        false);
    putCollection(
        digest,
        Objects.requireNonNull(
            command.payments().stream().map(value -> value.toString()).toList(), "payment values"),
        true);
    putCollection(
        digest,
        Objects.requireNonNull(
            command.additionalInformation().stream().map(value -> value.toString()).toList(),
            "additional-information values"),
        true);
    return Objects.requireNonNull(digest.digest());
  }

  public String hex(byte[] hash) {
    return Objects.requireNonNull(HexFormat.of().formatHex(hash));
  }

  private static void putCollection(
      MessageDigest digest, List<@NonNull String> values, boolean sorted) {
    List<@NonNull String> source =
        sorted ? values.stream().sorted(Comparator.naturalOrder()).toList() : values;
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(source.size()).array());
    source.forEach(value -> put(digest, Objects.requireNonNull(value)));
  }

  private static void putNullable(MessageDigest digest, @Nullable String value) {
    digest.update((byte) (value == null ? 0 : 1));
    if (value != null) {
      put(digest, value);
    }
  }

  private static void put(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }

  private static MessageDigest sha256() {
    try {
      return Objects.requireNonNull(MessageDigest.getInstance("SHA-256"));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the JVM", exception);
    }
  }
}
