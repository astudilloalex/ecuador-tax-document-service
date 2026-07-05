# AS-IS 08 - Integrations

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Integraciones detectadas por configuracion, imports y uso directo. Profundizacion de payloads queda pendiente.

| ID | System | Direction | Mechanism | Data Exchanged | Trigger | Error Handling | Evidence | Confidence |
|---|---|---|---|---|---|---|---|---|
| INT-001 | PostgreSQL | Internal DB | `pg.Pool` | usuarios, tenants, emisores, comprobantes, catalogos, auditoria, webhooks | Servicios/repositories | `ServiceUnavailableException`, logs DB, rollback tx | `src/database/database.service.ts:22-258`, `database/init.sql:39-1878` | High |
| INT-002 | Redis / BullMQ | Internal infra | BullMQ queues | jobs `sri-emision`, `webhook-dispatch` | Emision async, eventos webhook | attempts/backoff configured | `src/common/queues/queue.module.ts:15-71` | High |
| INT-003 | SRI SOAP | Outbound | `soap` clients / WSDL | XML comprobantes, claveAcceso, autorizaciones | Emision, autorizar, verificar, sincronizar | Errores en servicios SRI; detalle pendiente | `src/modules/sri/services/sri-soap-factory.service.ts:34-63`, `src/modules/sri/services/sri-soap.client.ts:30-60` | High |
| INT-004 | Carbone API | Outbound | HTTP via axios | templates/data/render result | Document/PDF generation | timeouts/errores de render pendiente | `src/config/configuration.ts:11-29`, `src/modules/document/document.service.ts:118-172`, `src/modules/pdf/pdf.service.ts:58-105` | High |
| INT-005 | Webhook consumers externos | Outbound | HTTP POST `fetch` | `{ evento, payload, timestamp }` + headers signature/event/attempt | Eventos comprobante autorizado/rechazado | log + rethrow para retry BullMQ | `src/modules/webhooks/webhook.processor.ts:37-107` | High |
| INT-006 | Filesystem local | Internal storage | `fs` read/write/mkdir | XML, PDF, templates, images, certs P12 | Generacion, descarga, firma, almacenamiento XML | NotFound/BadRequest/errores fs por modulo | `src/modules/sri/services/xml-storage.service.ts:23-138`, `src/modules/certificate/certificate.service.ts:78-216`, `src/modules/pdf/pdf.controller.ts:129-232` | High |
| INT-007 | Swagger UI | Inbound docs UI | `@nestjs/swagger` | contrato OpenAPI generado | Bootstrap | n/a | `src/main.ts:65-126` | High |

## Hallazgos De Integracion

INT-FIND-001: La URL de Carbone, WSDL SRI y credenciales/paths sensibles son configuracion requerida u opcional desde env centralizada; los valores no se reproducen.

**Evidence**
- File: `src/config/configuration.ts`
- Function / Method / Procedure: configuration factory
- Line / Section: lines 11-12, 40-52, 89-117
- Condition / Query / Statement: usa `requireEnv` para `CARBONE_API`, WSDL SRI, JWT/encryption/directorios; usa variables para cert path/password.
- Confidence: High

INT-FIND-002: Los webhooks salientes incluyen firma HMAC SHA-256 calculada con el secreto del webhook y enviada en header `X-Webhook-Signature`.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `WebhookProcessor.process`
- Line / Section: lines 37-58
- Condition / Query / Statement: body JSON, `crypto.createHmac('sha256', secreto)` y headers `X-Webhook-*`.
- Confidence: High

