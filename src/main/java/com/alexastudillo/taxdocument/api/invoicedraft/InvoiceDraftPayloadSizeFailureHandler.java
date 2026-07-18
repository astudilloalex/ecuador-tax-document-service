package com.alexastudillo.taxdocument.api.invoicedraft;

import jakarta.enterprise.context.ApplicationScoped;

/** Exclusive API construction of the payload-too-large outcome. */
@ApplicationScoped
public final class InvoiceDraftPayloadSizeFailureHandler {
  public ProblemDetails.ApiException payloadTooLarge() {
    return new ProblemDetails.ApiException(
        413, "REQUEST_PAYLOAD_TOO_LARGE", "The request body exceeds 2 MiB");
  }
}
