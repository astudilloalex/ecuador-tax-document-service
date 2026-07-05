# AS-IS 13 - Technical Debt

Ultima actualizacion: 2026-07-05

Estado: Parcial, lote 1. Se registran solo riesgos/deuda con evidencia directa, sin proponer redisenio.

## RISK-001 - Discrepancia entre README y rutas SRI implementadas

La documentacion publica rutas `/sri/factura/emitir` y analogas, pero el controlador implementa `/sri/emitir/factura` y analogas. Puede afectar clientes, Postman, pruebas y migracion de contrato.

**Evidence**
- File: `README.md`
- Function / Method / Procedure: API Endpoints / Ejemplo Real
- Line / Section: lines 171-176, 295-310
- Condition / Query / Statement: rutas documentadas en README.
- Confidence: Medium

**Evidence**
- File: `src/modules/sri/sri.controller.ts`
- Function / Method / Procedure: `SriController`
- Line / Section: lines 61, 84, 113, 140, 167, 194
- Condition / Query / Statement: rutas `@Post('emitir/*')`.
- Confidence: High

## RISK-002 - Credenciales iniciales documentadas/sembradas

El README y SQL inicial evidencian un usuario superadmin inicial. Por seguridad, valores de password/hash no se reproducen. Debe validarse politica de rotacion/uso en produccion.

**Evidence**
- File: `README.md`
- Function / Method / Procedure: "Credenciales iniciales"
- Line / Section: lines 470-479
- Condition / Query / Statement: documenta credenciales iniciales y advertencia de cambio.
- Confidence: Medium

**Evidence**
- File: `database/init.sql`
- Function / Method / Procedure: seed `usuarios`
- Line / Section: line 1015
- Condition / Query / Statement: inserta usuario superadmin inicial con `password_hash` redacted.
- Confidence: High

## RISK-003 - Contrato operativo depende de colas y filesystem

Emision SRI async y webhooks dependen de Redis/BullMQ; XML/PDF/certificados/templates dependen de rutas filesystem. Migracion debe preservar job types, retry/backoff y convenciones de almacenamiento.

**Evidence**
- File: `src/common/queues/queue.module.ts`
- Function / Method / Procedure: `QueueModule`
- Line / Section: lines 15-71
- Condition / Query / Statement: configura Redis y colas `sri-emision`, `webhook-dispatch`.
- Confidence: High

**Evidence**
- File: `src/modules/sri/services/xml-storage.service.ts`
- Function / Method / Procedure: `saveXml`, `readXml`
- Line / Section: lines 53-78, 135-138
- Condition / Query / Statement: filesystem para XML.
- Confidence: High

## RISK-004 - Scheduler interno no encontrado para sincronizacion SRI

La busqueda inicial no encontro scheduling interno; si operaciones dependen de cron externo, no esta documentado aun en codigo revisado.

**Evidence**
- File: `src/`, `package.json`, `docker-compose*.yml`
- Function / Method / Procedure: `rg -n "@Cron|ScheduleModule|cron|scheduler|schedule"`
- Line / Section: command output
- Condition / Query / Statement: no evidencia de scheduler NestJS/Cron; si hay proceso externo no se confirma en lote 1.
- Confidence: Medium

