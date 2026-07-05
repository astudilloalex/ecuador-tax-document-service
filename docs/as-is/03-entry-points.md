# AS-IS 03 - Entry Points

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Entry points derivados de decoradores NestJS y processors BullMQ. No se valida runtime.

| ID | Type | Name | Trigger | Inputs | Outputs | Module | Evidence | Confidence |
|---|---|---|---|---|---|---|---|---|
| EP-001 | HTTP app | Bootstrap / Swagger | Arranque NestJS y HTTP listen | Env/config | API en `port`, Swagger `/api` | MOD-001 | `src/main.ts:21-170` | High |
| EP-002 | HTTP GET public | `/status`, `/` | Request HTTP | none | Estado servidor / redirect status | MOD-011 | `src/modules/status/status.controller.ts:15-69` | High |
| EP-003 | HTTP POST public | `/auth/login` | Request HTTP | `LoginDto` | JWT access/refresh | MOD-002 | `src/modules/auth/auth.controller.ts:42-55` | High |
| EP-004 | HTTP POST public | `/auth/refresh` | Request HTTP | `RefreshTokenDto` | nuevos tokens | MOD-002 | `src/modules/auth/auth.controller.ts:61-73` | High |
| EP-005 | HTTP POST | `/auth/register` | Request HTTP | `RegisterUserDto` | usuario creado | MOD-002 | `src/modules/auth/auth.controller.ts:79-90` | High |
| EP-006 | HTTP GET | `/auth/me` | Request HTTP | JWT actual | perfil usuario | MOD-002 | `src/modules/auth/auth.controller.ts:97-108` | High |
| EP-007 | HTTP PATCH | `/auth/change-password` | Request HTTP | `ChangePasswordDto` | mensaje exito | MOD-002 | `src/modules/auth/auth.controller.ts:115-132` | High |
| EP-008 | HTTP POST | `/sri/emitir/factura` | Request HTTP | `CreateFacturaDto` | respuesta sync o job en cola | MOD-003 | `src/modules/sri/sri.controller.ts:84-110` | High |
| EP-009 | HTTP POST | `/sri/emitir/nota-credito` | Request HTTP | `CreateNotaCreditoDto` | respuesta sync o job en cola | MOD-003 | `src/modules/sri/sri.controller.ts:113-137` | High |
| EP-010 | HTTP POST | `/sri/emitir/nota-debito` | Request HTTP | `CreateNotaDebitoDto` | respuesta sync o job en cola | MOD-003 | `src/modules/sri/sri.controller.ts:140-164` | High |
| EP-011 | HTTP POST | `/sri/emitir/retencion` | Request HTTP | `CreateRetencionDto` | respuesta sync o job en cola | MOD-003 | `src/modules/sri/sri.controller.ts:167-191` | High |
| EP-012 | HTTP POST | `/sri/emitir/guia-remision` | Request HTTP | `CreateGuiaRemisionDto` | respuesta sync o job en cola | MOD-003 | `src/modules/sri/sri.controller.ts:194-218` | High |
| EP-013 | HTTP GET | `/sri/autorizar/:claveAcceso` | Request HTTP | clave acceso | estado autorizacion | MOD-003 | `src/modules/sri/sri.controller.ts:221-242` | High |
| EP-014 | HTTP POST | `/sri/preview/factura` | Request HTTP | `CreateFacturaDto` | XML sin firmar/enviar | MOD-003 | `src/modules/sri/sri.controller.ts:245-264` | High |
| EP-015 | HTTP POST multipart | `/sri/validar` | Upload HTTP | archivo `file` XML | `{valido, errores}` | MOD-003 | `src/modules/sri/sri.controller.ts:267-303` | High |
| EP-016 | HTTP POST | `/sri/debug/factura-firmada` | Request HTTP | `CreateFacturaDto` | clave, XML sin firma, XML firmado | MOD-003 | `src/modules/sri/sri.controller.ts:306-327` | High |
| EP-017 | HTTP GET | `/sri/comprobantes` | Request HTTP | filtros query | lista paginada | MOD-003 | `src/modules/sri/sri.controller.ts:334-375` | High |
| EP-018 | HTTP GET | `/sri/comprobantes/:claveAcceso` | Request HTTP | clave acceso | comprobante detallado | MOD-003 | `src/modules/sri/sri.controller.ts:378-399` | High |
| EP-019 | HTTP GET | `/sri/comprobantes/:claveAcceso/xml` | Request HTTP | clave acceso | XML attachment | MOD-003 | `src/modules/sri/sri.controller.ts:402-429` | High |
| EP-020 | HTTP PATCH | `/sri/comprobantes/:claveAcceso/anular` | Request HTTP | clave acceso | mensaje anulacion | MOD-003 | `src/modules/sri/sri.controller.ts:432-454` | High |
| EP-021 | HTTP POST | `/sri/comprobantes/:claveAcceso/reintentar` | Request HTTP | clave acceso | estado reenvio | MOD-003 | `src/modules/sri/sri.controller.ts:457-486` | High |
| EP-022 | HTTP GET | `/sri/verificar/:claveAcceso` | Request HTTP | clave acceso | estado SRI sin modificar BD | MOD-003 | `src/modules/sri/sri.controller.ts:489-519` | High |
| EP-023 | HTTP POST | `/sri/sincronizar` | Request HTTP | estados, reintentar, limite, rucEmisor | resumen sincronizacion | MOD-003 | `src/modules/sri/sri.controller.ts:522-591` | High |
| EP-024 | HTTP GET group | `/catalogos/*` | Request HTTP | none | catalogos SRI | MOD-004 | `src/modules/sri/catalogos.controller.ts:5-155` | High |
| EP-025 | HTTP CRUD | `/webhooks/*` | Request HTTP | DTOs webhooks | configs/logs/eventos | MOD-010 | `src/modules/webhooks/webhooks.controller.ts:31-164` | High |
| EP-026 | HTTP CRUD | `/tenants/*` | Request HTTP | tenant DTO/query | tenants | MOD-005 | `src/modules/tenants/tenants.controller.ts:29-90` | High |
| EP-027 | HTTP CRUD | `/emisores/*` | Request HTTP | emisor DTO/query | emisores | MOD-006 | `src/modules/emisores/emisores.controller.ts:29-109` | High |
| EP-028 | HTTP CRUD | `/emisores/puntos-emision/*` | Request HTTP | punto DTO/path | puntos emision | MOD-007 | `src/modules/puntos-emision/puntos-emision.controller.ts:28-127` | High |
| EP-029 | HTTP GET/PATCH | `/emisores/secuenciales/*` | Request HTTP | emisor/punto/tipo | secuenciales | MOD-007 | `src/modules/puntos-emision/secuenciales.controller.ts:15-78` | High |
| EP-030 | HTTP document/PDF | `/documents/*`, `/generate-pdf/*` | Request HTTP/upload | templates/data/files | docs/PDFs | MOD-009 | `src/modules/document/document.controller.ts:33-149`, `src/modules/pdf/pdf.controller.ts:48-440` | High |
| EP-031 | HTTP templates/images | `/templates/*`, `/images/*` | Request HTTP/upload | files/path | templates/images | MOD-009 | `src/modules/template/template.controller.ts:29-137`, `src/modules/image/image.controller.ts:30-139` | High |
| EP-032 | HTTP certificates | `/certificates/*` | Request HTTP/upload | P12/fileName/password | cert list/info/validation | MOD-008 | `src/modules/certificate/certificate.controller.ts:45-402` | High |
| EP-033 | HTTP signature | `/signature/*` | Request HTTP | PDF/template/sign config | PDF firmado | MOD-009 | `src/modules/signature/signature.controller.ts:27-264` | High |
| EP-034 | Queue processor | `sri-emision` | BullMQ job | `{ tipo, dto }` | service result / error | MOD-003 | `src/modules/sri/processors/sri-emision.processor.ts:10-47` | High |
| EP-035 | Queue processor | `webhook-dispatch` | BullMQ job | webhook job data | HTTP POST + log | MOD-010 | `src/modules/webhooks/webhook.processor.ts:20-144` | High |

## Discrepancias De Contrato Detectadas

EP-FIND-001: Las rutas SRI implementadas en codigo no coinciden con varias rutas publicadas en README.

**Evidence**
- File: `README.md`
- Function / Method / Procedure: API Endpoints / Ejemplo Real
- Line / Section: lines 171-176, 295-310
- Condition / Query / Statement: documenta rutas como `POST /sri/factura/emitir`.
- Confidence: Medium

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `SriController`
- Line / Section: lines 61, 84, 113, 140, 167, 194
- Condition / Query / Statement: implementa `@Controller('sri')` y rutas `emitir/*`, por ejemplo `POST /sri/emitir/factura`.
- Confidence: High

