# AS-IS 05 - Business Rules

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Reglas extraidas de controladores/servicios revisados. Reglas tributarias detalladas quedan pendientes.

## Reglas Confirmadas

### BR-001 - Emision SRI valida acceso al RUC del emisor

Los endpoints de emision de factura, nota credito, nota debito, retencion y guia de remision validan que el RUC del emisor sea accesible para el usuario autenticado antes de delegar al servicio SRI.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `emitirFactura`, `emitirNotaCredito`, `emitirNotaDebito`, `emitirRetencion`, `emitirGuiaRemision`
- Line / Section: lines 107-110, 135-137, 162-164, 189-191, 216-218
- Condition / Query / Statement: llama `emisoresService.validateRucAccess(dto.emisor.ruc, user)` antes de `sriService.emitir*`.
- Confidence: High

### BR-002 - Consulta/listado SRI restringe datos por tenant para usuarios no SUPERADMIN

En listado de comprobantes, si el usuario no es `SUPERADMIN`, el controlador valida `rucEmisor` o restringe a emisores del `tenantId`; si no hay emisores retorna lista vacia.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `listarComprobantes`
- Line / Section: lines 341-375
- Condition / Query / Statement: branch por `user.rol !== UserRole.SUPERADMIN`; valida RUC o filtra por `emisorIds`.
- Confidence: High

### BR-003 - Emision SRI es asincrona salvo configuracion explicita en falso

Cada metodo de emision revisado usa cola BullMQ si `SRI_EMISION_ASYNC` no es exactamente `'false'`; si es `'false'`, procesa sincronicamente delegando al servicio especifico.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `emitirFactura`, `emitirNotaCredito`, `emitirNotaDebito`, `emitirRetencion`, `emitirGuiaRemision`
- Line / Section: lines 56-67, 86-99, 106-119, 126-139, 146-159
- Condition / Query / Statement: `const isAsync = this.configService.get<string>('SRI_EMISION_ASYNC') !== 'false'`; encola `{ tipo, dto }`.
- Confidence: High

### BR-004 - Endpoint debug de factura firmada esta deshabilitado en produccion

El endpoint `POST /sri/debug/factura-firmada` lanza `ForbiddenException` si `NODE_ENV` es `production`.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `debugFacturaFirmada`
- Line / Section: lines 306-327
- Condition / Query / Statement: si `configService.get('NODE_ENV') === 'production'`, lanza `Endpoint deshabilitado en producción`.
- Confidence: High

### BR-005 - No se permite anular localmente comprobantes autorizados ni ya anulados

La anulacion local rechaza comprobantes inexistentes, `AUTORIZADO` y `ANULADO`; para comprobantes autorizados el mensaje indica que debe emitirse nota de credito.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `anularComprobante`
- Line / Section: lines 431-470
- Condition / Query / Statement: valida existencia, estado `AUTORIZADO` y `ANULADO`; actualiza estado a `ANULADO`.
- Confidence: High

### BR-006 - Reintento SRI solo aplica a estados reintentables y requiere XML firmado disponible

El reintento acepta estados `DEVUELTA`, `RECHAZADO`, `PENDIENTE`, `EN_PROCESO`, exige registro con `xml_firmado_path` y archivo legible antes de reenviar al SRI.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `reintentarComprobante`
- Line / Section: lines 477-536
- Condition / Query / Statement: valida estados, `xml_firmado_path`, lectura de XML y llama `sriSoapClient.enviarYAutorizar`.
- Confidence: High

### BR-007 - Sincronizacion consulta SRI antes de actualizar o reintentar

La sincronizacion toma estados por defecto `PENDIENTE`, `EN_PROCESO`, `DEVUELTA`; consulta SRI por lotes y actualiza a autorizado/rechazado o reintenta si no existe y `reintentar=true`.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `sincronizarConSri`
- Line / Section: lines 670-897
- Condition / Query / Statement: defaults de estados, limite global, consulta SRI, actualiza BD, guarda XML autorizado, emite eventos y opcionalmente reintenta.
- Confidence: High

### BR-008 - Webhooks se disparan solo para configuraciones activas suscritas al evento

Al recibir eventos `comprobante.autorizado` o `comprobante.rechazado`, el servicio busca `webhook_configs` activos donde el evento este en el arreglo `eventos`, opcionalmente filtrado por emisor, y encola cada dispatch.

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`
- Function / Method / Procedure: `handleComprobanteAutorizado`, `handleComprobanteRechazado`, `emit`
- Line / Section: lines 33-55, 271-321
- Condition / Query / Statement: query `WHERE activo = true AND $1 = ANY(eventos)` y `webhookQueue.add`.
- Confidence: High

