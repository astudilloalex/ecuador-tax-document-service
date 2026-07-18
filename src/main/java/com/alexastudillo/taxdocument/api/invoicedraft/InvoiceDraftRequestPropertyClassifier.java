package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/** Stage-5 classifier for client-supplied calculated properties before ordinary DTO binding. */
@ApplicationScoped
public final class InvoiceDraftRequestPropertyClassifier {
  private static final Set<String> ROOT_CALCULATED =
      Set.of("taxTotals", "subtotalBeforeTaxes", "totalDiscount", "grandTotal");
  private static final Set<String> LINE_CALCULATED =
      Set.of(
          "grossAmount",
          "netAmount",
          "lineTotal",
          "tax",
          "taxBase",
          "taxAmount",
          "taxCode",
          "taxRate",
          "officialTaxCode",
          "officialPercentageCode",
          "rate");

  public boolean containsCalculatedProperty(JsonNode request) {
    if (request == null || !request.isObject()) {
      return false;
    }
    for (String name : ROOT_CALCULATED) {
      if (request.has(name)) {
        return true;
      }
    }
    JsonNode lines = request.get("lines");
    if (lines != null && lines.isArray()) {
      for (JsonNode line : lines) {
        if (!line.isObject()) {
          continue;
        }
        for (String name : LINE_CALCULATED) {
          if (line.has(name)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
