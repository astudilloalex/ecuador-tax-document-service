package com.alexastudillo.taxdocument.application.invoicedraft;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;

/** Versioned, domain-separated privacy-minimal SHA-256 hashing. */
public final class IdempotencyFingerprint {
  public static final short NORMALIZATION_VERSION = 1;
  private static final byte[] KEY_DOMAIN =
      "invoice-draft:key:v1\0".getBytes(StandardCharsets.UTF_8);
  private static final byte[] REQUEST_DOMAIN =
      "invoice-draft:request:v1\0".getBytes(StandardCharsets.UTF_8);

  public byte[] keyHash(String normalizedKey) {
    Objects.requireNonNull(normalizedKey, "normalizedKey");
    MessageDigest digest = sha256();
    digest.update(KEY_DOMAIN);
    put(digest, normalizedKey);
    return digest.digest();
  }

  public byte[] requestFingerprint(CreateInvoiceDraftCommand command) {
    Objects.requireNonNull(command, "command");
    MessageDigest digest = sha256();
    digest.update(REQUEST_DOMAIN);
    put(digest, command.emissionPointId());
    put(digest, command.emissionDate().toString());
    put(digest, command.buyer().identificationType());
    put(digest, command.buyer().identification());
    put(digest, command.buyer().legalName());
    putNullable(digest, command.buyer().address());
    putNullable(digest, command.buyer().email());
    putNullable(digest, command.buyer().telephone());
    putCollection(digest, command.lines().stream().map(value -> value.toString()).toList(), false);
    putCollection(
        digest, command.payments().stream().map(value -> value.toString()).toList(), true);
    putCollection(
        digest,
        command.additionalInformation().stream().map(value -> value.toString()).toList(),
        true);
    return digest.digest();
  }

  public String hex(byte[] hash) {
    return HexFormat.of().formatHex(hash);
  }

  private static void putCollection(
      MessageDigest digest, Collection<String> values, boolean sorted) {
    Collection<String> source =
        sorted ? values.stream().sorted(Comparator.naturalOrder()).toList() : values;
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(source.size()).array());
    source.forEach(value -> put(digest, value));
  }

  private static void putNullable(MessageDigest digest, String value) {
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
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the JVM", exception);
    }
  }
}
