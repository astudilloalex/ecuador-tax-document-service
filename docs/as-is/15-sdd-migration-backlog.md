# AS-IS 15 - SDD Migration Backlog

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Items de preservacion de comportamiento; no prescriben arquitectura ni implementacion destino.

## MIG-001 - Preservar contrato efectivo de rutas y resolver discrepancia documental

Comportamiento a preservar: las rutas HTTP efectivamente implementadas deben identificarse antes de generar requisitos destino; la discrepancia con README debe resolverse por validacion funcional.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`, `README.md`
- Function / Method / Procedure: `SriController`, API Endpoints README
- Line / Section: `src/modules/sri/sri.controller.ts:84-218`, `README.md:295-310`
- Condition / Query / Statement: rutas implementadas `emitir/*` vs rutas documentadas `*/emitir`.
- Confidence: Medium

Dependencies: PFV-001.
Risks: RISK-001.
Acceptance considerations: pruebas de contrato deben cubrir rutas vigentes confirmadas por stakeholders/codigo.

## MIG-002 - Preservar autenticacion JWT, excepciones publicas y roles

Comportamiento a preservar: guard JWT global por defecto, `@Public()` para login/refresh/status, roles via `@Roles` y rechazo de refresh tokens para recursos.

**Evidence**
- File: `src/app.module.ts`, `src/modules/auth/**`
- Function / Method / Procedure: guards, controller, service
- Line / Section: `src/app.module.ts:120-140`, `src/modules/auth/auth.controller.ts:42-63`, `src/modules/auth/auth.service.ts:223-239`
- Condition / Query / Statement: guards globales, public routes, refresh token denial.
- Confidence: High

Dependencies: none identified in lote 1.
Risks: acceso indebido o endpoints bloqueados si se altera default global.
Acceptance considerations: tests deben cubrir anonimo, access token, refresh token y roles.

## MIG-003 - Preservar aislamiento multi-tenant por RUC/emisor

Comportamiento a preservar: emision y consultas por clave/RUC deben validar acceso al emisor; usuarios no SUPERADMIN se restringen por tenant/emisores.

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `emitir*`, `listarComprobantes`, `validateClaveAccesoAccess`, `sincronizar`
- Line / Section: lines 76-82, 107-110, 341-375, 582-590
- Condition / Query / Statement: validacion RUC/tenant y restricciones no SUPERADMIN.
- Confidence: High

Dependencies: `emisores`, `tenants`, `usuarios`.
Risks: fuga de comprobantes entre tenants.
Acceptance considerations: pruebas multi-tenant con SUPERADMIN, ADMIN/USER con/sin `rucEmisor`.

## MIG-004 - Preservar modo de emision SRI sync/async y job contract

Comportamiento a preservar: `SRI_EMISION_ASYNC !== 'false'` encola jobs `sri-emision` con `{ tipo, dto }`; modo sync delega directamente.

**Evidence**
- File: `src/modules/sri/sri.service.ts`, `src/modules/sri/processors/sri-emision.processor.ts`
- Function / Method / Procedure: `emitir*`, `SriEmisionProcessor.process`
- Line / Section: `src/modules/sri/sri.service.ts:56-159`, `src/modules/sri/processors/sri-emision.processor.ts:24-47`
- Condition / Query / Statement: branch async/sync y switch por `tipo`.
- Confidence: High

Dependencies: Redis/BullMQ, servicios de comprobantes.
Risks: respuestas HTTP o reintentos cambian frente a clientes actuales.
Acceptance considerations: validar respuestas encoladas, tipos soportados y manejo de tipo no soportado.

## MIG-005 - Preservar reglas de anulacion, reintento y sincronizacion

Comportamiento a preservar: anulacion local no permite `AUTORIZADO`/`ANULADO`; reintento requiere estado permitido y XML firmado; sincronizacion consulta SRI antes de actualizar/reintentar.

**Evidence**
- File: `src/modules/sri/sri.service.ts`
- Function / Method / Procedure: `anularComprobante`, `reintentarComprobante`, `sincronizarConSri`
- Line / Section: lines 431-470, 477-587, 670-897
- Condition / Query / Statement: validaciones de estado, XML firmado y flujo de sync.
- Confidence: High

Dependencies: DB comprobantes/XML, SRI SOAP, filesystem.
Risks: duplicacion/envio incorrecto de comprobantes o anulacion contraria a proceso actual.
Acceptance considerations: escenarios por estado SRI/local y ausencia de XML.

## MIG-006 - Preservar persistencia relacional principal

Comportamiento a preservar: entidades fisicas, constraints e indices de comprobantes, emisores, tenants, usuarios, catalogos, secuenciales, XML y webhooks deben mapearse antes de requisitos destino.

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: DDL schema
- Line / Section: lines 39-1878
- Condition / Query / Statement: tablas, constraints, FKs e indices.
- Confidence: High

Dependencies: `database/init.sql`, repositories/services.
Risks: perdida de historial, claves funcionales o integridad referencial.
Acceptance considerations: matriz tabla-caso de uso y migracion de datos con constraints equivalentes.

## MIG-007 - Preservar salidas filesystem y referencias DB

Comportamiento a preservar: XML firmado/autorizado y archivos generados se almacenan en filesystem con rutas persistidas o expuestas.

**Evidence**
- File: `database/init.sql`, `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `comprobante_xmls`, `saveXml`, `readXml`
- Line / Section: `database/init.sql:327-347`, `src/modules/sri/services/xml-storage.service.ts:53-78`
- Condition / Query / Statement: rutas de XML en BD y filesystem.
- Confidence: High

Dependencies: XML/PDF/cert/template directories.
Risks: enlaces/descargas historicas rotas tras migracion.
Acceptance considerations: pruebas de descarga XML/PDF y compatibilidad de rutas.

## MIG-008 - Preservar webhooks salientes y semantica de retry/log

Comportamiento a preservar: eventos autorizados/rechazados, filtro por configuracion activa/evento/emisor, body/headers firmados HMAC, logs y retries BullMQ.

**Evidence**
- File: `src/modules/webhooks/webhooks.service.ts`, `src/modules/webhooks/webhook.processor.ts`
- Function / Method / Procedure: `emit`, `WebhookProcessor.process`, `logWebhook`
- Line / Section: `src/modules/webhooks/webhooks.service.ts:271-321`, `src/modules/webhooks/webhook.processor.ts:37-107`, `src/modules/webhooks/webhook.processor.ts:111-137`
- Condition / Query / Statement: query de configs, queue add, HMAC, fetch, insert log, rethrow errors.
- Confidence: High

Dependencies: PFV-006, webhook consumers externos.
Risks: integraciones externas dejan de verificar firma o recibir eventos.
Acceptance considerations: contrato de body/headers, logs por intento y retry en no-OK.
