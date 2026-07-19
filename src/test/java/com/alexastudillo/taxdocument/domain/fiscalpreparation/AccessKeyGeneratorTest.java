package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class AccessKeyGeneratorTest {
  private static final Path VECTORS =
      requireNonNull(
          Path.of("src/test/resources/fiscalpreparation/sri-access-key-v2.33-vectors.json"));
  private final AccessKeyGenerator generator = new AccessKeyGenerator();

  @Test
  void consumesEveryIndependentlyRecalculatedSriV233PositiveVector() throws Exception {
    JsonNode root = new ObjectMapper().readTree(VECTORS.toFile());
    for (JsonNode candidateVector : root.required("positive")) {
      JsonNode vector = requireNonNull(candidateVector);
      AccessKey generated =
          generator.generate(
              parseDate(vector),
              requiredText(vector, "issuerRuc"),
              requiredText(vector, "environmentCode"),
              requiredText(vector, "establishmentCode"),
              requiredText(vector, "emissionPointCode"),
              OfficialSequentialNumber.parse(requiredText(vector, "officialSequentialNumber")),
              NumericCode.parse(requiredText(vector, "numericCode")));
      assertEquals(vector.required("accessKey").asText(), generated.value());
      assertEquals(vector.required("verificationDigit").asInt(), generated.verificationDigit());
      generator.validateMatches(
          generated,
          parseDate(vector),
          requiredText(vector, "issuerRuc"),
          requiredText(vector, "environmentCode"),
          requiredText(vector, "establishmentCode"),
          requiredText(vector, "emissionPointCode"),
          OfficialSequentialNumber.parse(requiredText(vector, "officialSequentialNumber")),
          NumericCode.parse(requiredText(vector, "numericCode")));
    }
  }

  @Test
  void appliesRightToLeftWeightsAndBothOfficialModuloElevenMappings() throws Exception {
    JsonNode root = new ObjectMapper().readTree(VECTORS.toFile());
    assertEquals(6, AccessKeyGenerator.verificationDigit("41261533"));
    for (JsonNode edge : root.required("moduloEdges")) {
      String key = edge.required("accessKey").asText();
      assertEquals(
          edge.required("mapped").asInt(),
          AccessKeyGenerator.verificationDigit(key.substring(0, 48)));
      assertEquals(key, AccessKey.parse(key).value());
    }
  }

  @Test
  void rejectsThePage64PrintedDigitAndEveryComponentOrCheckDigitMutation() throws Exception {
    JsonNode root = new ObjectMapper().readTree(VECTORS.toFile());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AccessKey.parse(
                requiredText(requireNonNull(root.required("page64PrintedNegative")), "printed")));

    JsonNode vector = root.required("positive").get(0);
    String valid = vector.required("accessKey").asText();
    for (JsonNode index : root.required("componentMutationIndices")) {
      int position = index.asInt();
      char replacement = valid.charAt(position) == '9' ? '8' : (char) (valid.charAt(position) + 1);
      String mutated = valid.substring(0, position) + replacement + valid.substring(position + 1);
      if (position == 48) {
        assertThrows(IllegalArgumentException.class, () -> AccessKey.parse(mutated));
      } else {
        AccessKey selfConsistent =
            AccessKey.parse(
                mutated.substring(0, 48)
                    + AccessKeyGenerator.verificationDigit(mutated.substring(0, 48)));
        assertThrows(
            IllegalArgumentException.class,
            () ->
                generator.validateMatches(
                    selfConsistent,
                    parseDate(vector),
                    requiredText(vector, "issuerRuc"),
                    requiredText(vector, "environmentCode"),
                    requiredText(vector, "establishmentCode"),
                    requiredText(vector, "emissionPointCode"),
                    OfficialSequentialNumber.parse(
                        requiredText(vector, "officialSequentialNumber")),
                    NumericCode.parse(requiredText(vector, "numericCode"))));
      }
    }
  }

  private static LocalDate parseDate(JsonNode vector) {
    return requireNonNull(LocalDate.parse(requiredText(vector, "emissionDate")));
  }

  private static String requiredText(JsonNode node, String field) {
    return requireNonNull(node.required(field).asText(), field);
  }
}
