/**
 * Outbound persistence adapter implementations for application ports.
 *
 * <p>This package may depend on persistence frameworks and database-specific
 * details. Those details must not leak into domain or application layers.
 *
 * <p>The package owns persistence entities, mappers, repository adapters,
 * sequence reservation adapters, transaction adapters, and database exception
 * translation. It must not contain business issuance policy, REST resources,
 * SRI XML/SOAP behavior, XML storage, queue adapters, webhook delivery, or
 * bootstrap runtime wiring.
 */
package com.alexastudillo.taxdocument.adapter.out.persistence;
