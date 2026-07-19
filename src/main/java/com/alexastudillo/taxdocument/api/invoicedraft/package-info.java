/**
 * HTTP boundary for the Invoice Draft capability.
 *
 * <p>This package owns transport validation, the request deadline and correlation lifecycle, and
 * explicit mapping to application inputs and outputs. It accepts Company context only through
 * {@code X-Company-Id} and contains no persistence models, authentication, authorization, Company
 * client, or fiscal-issuance integration.
 */
package com.alexastudillo.taxdocument.api.invoicedraft;
