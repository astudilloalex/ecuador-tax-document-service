package com.alexastudillo.taxdocument.api.invoicedraft.telemetry;

import com.alexastudillo.taxdocument.api.invoicedraft.InvoiceDraftRequestState;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftResult;

/** API-facing operational evidence boundary implemented by Infrastructure. */
public interface InvoiceDraftTelemetryPort {
  void completed(
      InvoiceDraftRequestState state, CreateInvoiceDraftResult result, int selectedStatus);

  void failed(InvoiceDraftRequestState state, String outcome, int selectedStatus);

  void lateOutcome(InvoiceDraftRequestState state, String outcome);
}
