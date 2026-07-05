# AS-IS 07 - Process Flows

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Flujos de alto nivel confirmados; pasos internos de generacion XML/firma por comprobante quedan pendientes.

## DF-001 - Emision SRI HTTP a servicio/cola

1. Cliente invoca endpoint `/sri/emitir/*` con DTO del comprobante.
2. Controlador valida acceso al RUC del emisor.
3. Servicio SRI decide asincronia segun `SRI_EMISION_ASYNC`.
4. Si async, encola job `sri-emision` con `tipo` y `dto` y retorna `estado: EN_COLA`.
5. Si sync, delega al servicio del tipo de comprobante.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `emitirFactura` y analogos
- Line / Section: lines 84-218
- Condition / Query / Statement: endpoints de emision validan RUC y llaman `sriService.emitir*`.
- Confidence: High

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `emitirFactura` y analogos
- Line / Section: lines 56-159
- Condition / Query / Statement: branch async/sync y `emisionQueue.add('emision', { tipo, dto })`.
- Confidence: High

## DF-002 - Procesamiento asincrono `sri-emision`

1. Worker recibe job BullMQ en cola `sri-emision`.
2. Lee `tipo` y `dto`.
3. Despacha al servicio especifico (`FacturaService`, `NotaCreditoService`, `NotaDebitoService`, `RetencionService`, `GuiaRemisionService`).
4. Si `tipo` no esta soportado, lanza error.

**Evidence**
- File: `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `SriEmisionProcessor.process`
- Line / Section: lines 24-47
- Condition / Query / Statement: `switch (tipo)` por tipos soportados y `throw new Error` para default.
- Confidence: High

## DF-003 - Sincronizacion manual con SRI

1. Cliente invoca `POST /sri/sincronizar`.
2. Si usuario no es SUPERADMIN, debe enviar `rucEmisor` y pasar validacion de acceso.
3. Servicio consulta comprobantes por estados y limite.
4. Por cada comprobante consulta autorizacion SRI.
5. Si SRI devuelve autorizado, actualiza BD, guarda XML autorizado y emite evento.
6. Si SRI devuelve rechazado/devuelta/no autorizado, actualiza estado y emite evento rechazado.
7. Si no existe en SRI y `reintentar=true`, intenta reenvio.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `sincronizar`
- Line / Section: lines 522-591
- Condition / Query / Statement: valida `rucEmisor` para no SUPERADMIN y delega a `sincronizarConSri`.
- Confidence: High

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `sincronizarConSri`
- Line / Section: lines 693-897
- Condition / Query / Statement: obtiene lotes, consulta SRI, actualiza estados, guarda XML, emite eventos y registra detalle.
- Confidence: High

## DF-004 - Dispatch de webhooks

1. Servicios SRI emiten eventos de comprobante autorizado/rechazado.
2. `WebhooksService` escucha eventos.
3. Busca webhooks activos suscritos.
4. Encola un job `webhook-dispatch` por configuracion.
5. Processor envia POST externo firmado con HMAC SHA-256 y registra log.
6. Si la respuesta HTTP no es OK, lanza error para que BullMQ reintente.

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: event handlers y `emit`
- Line / Section: lines 33-55, 271-321
- Condition / Query / Statement: `@OnEvent`, query de configs activas y `webhookQueue.add`.
- Confidence: High

**Evidence**
- File: `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `process`, `logWebhook`
- Line / Section: lines 28-107, 111-143
- Condition / Query / Statement: construye body, firma HMAC, `fetch`, registra log y relanza errores.
- Confidence: High

