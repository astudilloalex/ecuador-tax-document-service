# AS-IS 02 - Module Inventory

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Responsabilidades extraidas de `AppModule`, nombres de modulos/controladores, README y lectura puntual de SRI/Auth/Webhooks/DB/colas.

| ID | Module | Responsibility | Main Files | Entry Points | Data Dependencies | Evidence | Confidence |
|---|---|---|---|---|---|---|---|
| MOD-001 | Application Shell / Bootstrap | Arranque NestJS, seguridad HTTP, CORS, validacion global, Swagger, archivos estaticos y shutdown hooks. | `src/main.ts`, `src/app.module.ts`, `src/config/configuration.ts` | EP-001 | Env, filesystem dirs | `src/main.ts:21-170`, `src/app.module.ts:41-143` | High |
| MOD-002 | Auth | Login, refresh, registro de usuarios, perfil, cambio de password, JWT y roles. | `src/modules/auth/**` | EP-003..EP-007 | `usuarios`, `tenants` | `src/modules/auth/auth.controller.ts:31-134`, `src/modules/auth/auth.service.ts:35-239` | High |
| MOD-003 | SRI Facturacion Electronica | Emision, consulta, validacion, anulacion local, reintento y sincronizacion de comprobantes SRI. | `src/modules/sri/**` | EP-008..EP-023, EP-034 | `comprobantes`, `comprobante_*`, `emisores`, XML filesystem, SRI SOAP, Redis | `src/modules/sri/sri.controller.ts:59-593`, `src/modules/sri/sri.service.ts:56-897` | High |
| MOD-004 | Catalogos SRI | Exposicion de catalogos de impuestos, retenciones, formas de pago, identificacion, documentos sustento y motivos traslado. | `src/modules/sri/catalogos.controller.ts`, `database/init.sql` | EP-024 | catalogos SQL | `src/modules/sri/catalogos.controller.ts:5-155`, `database/init.sql:100-198` | High |
| MOD-005 | Tenants | Gestion administrativa de tenants multi-tenant, restringida a SUPERADMIN por controlador. | `src/modules/tenants/**` | EP-026 | `tenants` | `src/modules/tenants/tenants.controller.ts:29-90`, `database/init.sql:684` | High |
| MOD-006 | Emisores | Gestion de empresas emisoras y validacion de acceso por RUC usada por SRI. | `src/modules/emisores/**` | EP-027 | `emisores`, `tenants` | `src/modules/emisores/emisores.controller.ts:29-109`, `src/modules/sri/sri.controller.ts:107-110` | High |
| MOD-007 | Puntos de Emision / Secuenciales | Gestion de puntos de emision y secuenciales por emisor/punto/tipo comprobante. | `src/modules/puntos-emision/**` | EP-028, EP-029 | `establecimientos`, `puntos_emision`, `secuenciales` | `src/modules/puntos-emision/puntos-emision.controller.ts:28-127`, `src/modules/puntos-emision/secuenciales.controller.ts:15-78` | High |
| MOD-008 | Certificates | Gestion de certificados P12: listar, subir, eliminar, info y validacion. | `src/modules/certificate/**` | EP-032 | filesystem certs, `emisores` metadata pendiente | `src/modules/certificate/certificate.controller.ts:45-402` | Medium |
| MOD-009 | Documents/PDF/Templates/Images/Signature | Generacion/guardado/descarga de documentos y PDFs, templates, imagenes y firma de PDFs. | `src/modules/document/**`, `src/modules/pdf/**`, `src/modules/template/**`, `src/modules/image/**`, `src/modules/signature/**` | EP-030, EP-031, EP-033 | filesystem templates/pdfs/images, Carbone API | `README.md:331-367`, controladores segun `rg` | Medium |
| MOD-010 | Webhooks | CRUD de webhooks, eventos, logs y dispatch asincrono con firma HMAC. | `src/modules/webhooks/**` | EP-025, EP-035 | `webhook_configs`, `webhook_logs`, Redis, HTTP externo | `src/modules/webhooks/webhooks.service.ts:33-321`, `src/modules/webhooks/webhook.processor.ts:28-144` | High |
| MOD-011 | Status / Health | Estado operacional publico y health checks de dependencias. | `src/modules/status/**` | EP-002 | DB, Redis, filesystem/config | `src/modules/status/status.controller.ts:15-69`, `src/modules/status/status.service.ts:26` | High |
| MOD-012 | Database | Pool PostgreSQL, query helpers, transacciones, sanitizacion de identificadores. | `src/database/**` | Interno | PostgreSQL | `src/database/database.service.ts:12-258` | High |
| MOD-013 | Queue | Configuracion global BullMQ/Redis y colas `sri-emision`, `webhook-dispatch`. | `src/common/queues/queue.module.ts` | EP-034, EP-035 | Redis | `src/common/queues/queue.module.ts:11-75` | High |
| MOD-014 | Common cross-cutting | Filtro global de errores, auditoria, cifrado, cache Redis e interfaces comunes. | `src/common/**` | Interno | DB/Redis/env segun servicio | `src/app.module.ts:94-99`, `src/common/filters/http-exception.filter.ts:11-46` | Medium |

## Hallazgos De Modulos

MOD-FIND-001: El sistema se ensambla como aplicacion modular NestJS con guards globales JWT/roles/throttling y auditoria global.

**Evidence**
- File: `src/app.module.ts`
- Function / Method / Procedure: `AppModule`
- Line / Section: lines 120-140
- Condition / Query / Statement: registra `JwtAuthGuard`, `RolesGuard`, `ThrottlerGuard` como `APP_GUARD` y `AuditInterceptor` como `APP_INTERCEPTOR`.
- Confidence: High

MOD-FIND-002: El modulo SRI concentra el dominio principal y exporta servicios reutilizables de XML, SOAP, firma, catalogos y repositorio.

**Evidence**
- File: `src/modules/sri/sri.module.ts`
- Function / Method / Procedure: `SriModule`
- Line / Section: lines 33-68
- Condition / Query / Statement: controllers `SriController`, `CatalogosController`; providers y exports para servicios SRI.
- Confidence: High
