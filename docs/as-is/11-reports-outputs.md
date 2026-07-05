# AS-IS 11 - Reports Outputs

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Salidas detalladas de PDF/documentos/certificados quedan pendientes.

## Outputs Confirmados

### REP-001 - Respuesta de emision encolada

Cuando la emision SRI es asincrona, retorna mensaje, `jobId` y `estado: EN_COLA`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `emitirFactura` y analogos
- Line / Section: lines 61-67, 93-99, 113-119, 133-139, 153-159
- Condition / Query / Statement: retorna objeto con mensaje, jobId y estado.
- Confidence: High

### REP-002 - XML autorizado descargable

`GET /sri/comprobantes/:claveAcceso/xml` retorna XML con `Content-Type: application/xml` y attachment con nombre `{claveAcceso}.xml`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `descargarXml`
- Line / Section: lines 402-429
- Condition / Query / Statement: setea headers `Content-Type` y `Content-Disposition`; envia XML.
- Confidence: High

### REP-003 - Vista previa XML de factura

`POST /sri/preview/factura` devuelve `{ xml }` sin firmar ni enviar.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `previewFactura`
- Line / Section: lines 245-264
- Condition / Query / Statement: `const xml = this.sriService.generarXmlPreview(dto); return { xml }`.
- Confidence: High

### REP-004 - Debug factura firmada

`POST /sri/debug/factura-firmada` devuelve `claveAcceso`, `xmlSinFirma` y `xmlFirmado`, pero esta bloqueado en produccion.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `debugFacturaFirmada`
- Line / Section: lines 306-327
- Condition / Query / Statement: response type con tres campos; lanza Forbidden en produccion.
- Confidence: High

### REP-005 - Logs de webhooks

Cada intento de webhook registra evento, payload, status/respuesta, intento, exito/error y tiempo de respuesta en `webhook_logs`.

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `logWebhook`
- Line / Section: lines 111-137
- Condition / Query / Statement: `INSERT INTO webhook_logs (...) VALUES (...)`.
- Confidence: High

### REP-006 - Archivos generados en filesystem

El sistema genera/lee XML, PDFs/documentos, templates, imagenes y certificados en filesystem segun modulos revisados o detectados.

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `saveXml`, `readXml`
- Line / Section: lines 53-78, 135-138
- Condition / Query / Statement: write/read de XML.
- Confidence: High

**Evidence**
- File: `src/modules/pdf/pdf.controller.ts`, `src/modules/document/document.controller.ts`, `src/modules/template/template.service.ts`
- Function / Method / Procedure: filesystem operations
- Line / Section: `src/modules/pdf/pdf.controller.ts:129-232`, `src/modules/document/document.controller.ts:201-209`, `src/modules/template/template.service.ts:43`
- Condition / Query / Statement: escritura/creacion/listado de archivos para PDFs/documentos/templates.
- Confidence: Medium

